package com.example.mangareader.download

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.mangareader.data.MangaRepository
import com.example.mangareader.data.local.db.AppDatabase
import com.example.mangareader.data.local.db.entity.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull // Import for Flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val mangaRepository = MangaRepository() // In a real app, inject with Hilt/Koin
    private val chapterDao = AppDatabase.getDatabase(applicationContext).downloadedChapterDao()
    // private val mangaDao = AppDatabase.getDatabase(applicationContext).downloadedMangaDao() // Needed if creating manga entry
    private val httpClient = OkHttpClient() // Simple client for downloads

    companion object {
        const val KEY_CHAPTER_ID = "chapter_id"
        const val KEY_MANGA_ID = "manga_id" // Needed to create chapter download path
        // These are passed to DownloadManager, not strictly needed by Worker if DB is source of truth
        // const val KEY_MANGA_TITLE = "manga_title"
        // const val KEY_CHAPTER_NUMBER = "chapter_number"
        // const val KEY_CHAPTER_TITLE = "chapter_title"


        const val TAG = "DownloadWorker"
        const val PROGRESS = "Progress"
    }

    override suspend fun doWork(): Result {
        val chapterId = inputData.getString(KEY_CHAPTER_ID) ?: return Result.failure()
        val mangaId = inputData.getString(KEY_MANGA_ID) ?: return Result.failure()

        Log.d(TAG, "Worker started for chapter ID: $chapterId, Manga ID: $mangaId")

        // Fetch chapter details from DB to get expected page count and verify
        val chapterEntity = chapterDao.getChapterById(chapterId).firstOrNull()
        if (chapterEntity == null) {
            Log.e(TAG, "Chapter $chapterId not found in DB for download. Failing worker.")
            return Result.failure()
        }
        // If already downloaded, succeed immediately.
        if (chapterEntity.downloadStatus == DownloadStatus.DOWNLOADED) {
             Log.i(TAG, "Chapter $chapterId already marked as DOWNLOADED. Worker succeeding.")
             setProgress(workDataOf(PROGRESS to 100))
             return Result.success()
        }

        val expectedPageCount = chapterEntity.pageCount // pageCount should be set when enqueued
        if (expectedPageCount == 0) {
            // This case should ideally be prevented by DownloadManager ensuring pageCount > 0
            Log.w(TAG, "Chapter $chapterId has 0 expected pages. Marking as failed.")
            chapterDao.updateChapterStatus(chapterId, DownloadStatus.FAILED)
            return Result.failure()
        }


        Log.d(TAG, "Starting download for chapter ID: $chapterId, Expected Pages: $expectedPageCount")
        setProgress(workDataOf(PROGRESS to 0)) // Initial progress

        try {
            chapterDao.updateChapterStatus(chapterId, DownloadStatus.DOWNLOADING)
            val pageUrlsResult = mangaRepository.getChapterPageImageUrls(chapterId)

            pageUrlsResult.onSuccess { pageUrls ->
                if (pageUrls.isEmpty()) {
                    Log.w(TAG, "No page URLs found for chapter $chapterId via repository.")
                    chapterDao.updateChapterStatus(chapterId, DownloadStatus.FAILED)
                    return Result.failure()
                }

                val chapterDir = File(applicationContext.filesDir, "downloads/$mangaId/$chapterId")
                if (!chapterDir.exists() && !chapterDir.mkdirs()) {
                    Log.e(TAG, "Failed to create directory: ${chapterDir.absolutePath}")
                    chapterDao.updateChapterStatus(chapterId, DownloadStatus.FAILED)
                    return Result.failure()
                }
                // Update chapter entity with the actual download path if not already set or different
                if (chapterEntity.chapterDownloadPath != chapterDir.absolutePath) {
                     chapterDao.updateChapter(chapterEntity.copy(chapterDownloadPath = chapterDir.absolutePath))
                }

                var downloadedPageCount = 0
                for ((index, pageUrl) in pageUrls.withIndex()) {
                    if (isStopped) {
                        Log.d(TAG, "Download cancelled for chapter $chapterId")
                        chapterDao.updateChapterStatus(chapterId, DownloadStatus.PAUSED)
                        return Result.failure()
                    }

                    val pageFileName = "page_${index + 1}.${pageUrl.substringAfterLast('.', "jpg")}"
                    val pageFile = File(chapterDir, pageFileName)

                    try {
                        downloadImage(pageUrl, pageFile)
                        downloadedPageCount++
                        chapterDao.updateChapterDownloadedPages(chapterId, downloadedPageCount)
                        val progress = (downloadedPageCount * 100 / pageUrls.size)
                        setProgress(workDataOf(PROGRESS to progress))
                        Log.d(TAG, "Downloaded page ${index + 1}/${pageUrls.size} for chapter $chapterId")

                    } catch (e: IOException) {
                        Log.e(TAG, "Failed to download page $pageUrl: ${e.message}", e)
                        chapterDao.updateChapterStatus(chapterId, DownloadStatus.FAILED)
                        return Result.failure()
                    }
                }

                if (downloadedPageCount == pageUrls.size && downloadedPageCount >= expectedPageCount) {
                    chapterDao.updateChapterStatus(chapterId, DownloadStatus.DOWNLOADED)
                    Log.d(TAG, "Chapter $chapterId downloaded successfully. ($downloadedPageCount/$expectedPageCount pages)")
                    setProgress(workDataOf(PROGRESS to 100))
                    return Result.success()
                } else {
                     Log.w(TAG, "Chapter $chapterId download mismatch. API URLs: ${pageUrls.size}, Expected in DB: $expectedPageCount, Actually Downloaded: $downloadedPageCount.")
                    // If fewer pages downloaded than expected, mark as failed.
                    // If more URLs from API than in DB, it's also an issue; perhaps it should be FAILED or PARTIAL.
                    chapterDao.updateChapterStatus(chapterId, DownloadStatus.FAILED)
                    return Result.failure()
                }

            }.onFailure { error ->
                Log.e(TAG, "Failed to get page URLs for $chapterId: ${error.message}", error)
                chapterDao.updateChapterStatus(chapterId, DownloadStatus.FAILED)
                return Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during download for chapter $chapterId: ${e.message}", e)
            chapterDao.updateChapterStatus(chapterId, DownloadStatus.FAILED)
            return Result.failure()
        }
        // Fallback, should ideally be handled by specific success/failure paths above
        Log.e(TAG, "Reached end of doWork for $chapterId without explicit success/failure. Marking as FAILED.")
        chapterDao.updateChapterStatus(chapterId, DownloadStatus.FAILED)
        return Result.failure()
    }

    private suspend fun downloadImage(url: String, outputFile: File) {
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code ${response.code} from $url")

                FileOutputStream(outputFile).use { outputStream ->
                    response.body!!.source().use { source ->
                        source.readAll(outputStream)
                    }
                }
            }
        }
    }
}
