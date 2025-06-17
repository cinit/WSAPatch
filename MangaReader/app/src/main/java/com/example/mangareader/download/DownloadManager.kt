package com.example.mangareader.download

import android.content.Context
import android.util.Log // Import Log
import androidx.work.*
import com.example.mangareader.data.local.db.AppDatabase
import com.example.mangareader.data.local.db.entity.DownloadedChapterEntity
import com.example.mangareader.data.local.db.entity.DownloadedMangaEntity
import com.example.mangareader.data.local.db.entity.DownloadStatus
import com.example.mangareader.data.model.ChapterInfo
import com.example.mangareader.data.model.MangaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File


class DownloadManager(private val context: Context) {
    private val workManager = WorkManager.getInstance(context)
    private val chapterDao = AppDatabase.getDatabase(context).downloadedChapterDao()
    private val mangaDao = AppDatabase.getDatabase(context).downloadedMangaDao()


    suspend fun enqueueChapterDownload(manga: MangaInfo, chapter: ChapterInfo) {
        // 1. Ensure Manga entry exists in DB (or create it)
        var mangaEntity = mangaDao.getMangaById(manga.id).firstOrNull()
        if (mangaEntity == null) {
            mangaEntity = DownloadedMangaEntity(
                mangaId = manga.id,
                title = manga.title,
                coverImageUrl = manga.coverImageUrl,
                localCoverImagePath = null, // Cover download can be separate
                source = "MangaDex" // Or determine source dynamically
            )
            mangaDao.insertManga(mangaEntity)
            Log.i(DownloadWorker.TAG, "Inserted new manga into DB: ${manga.title}")
        }

        // 2. Create or update Chapter entry in DB with QUEUED status and page count
        var chapterEntity = chapterDao.getChapterById(chapter.id).firstOrNull()

        if (chapter.pageCount == null || chapter.pageCount == 0) {
            Log.e(DownloadWorker.TAG, "Chapter ${chapter.id} has no page count. Cannot enqueue download.")
            // Optionally, update DB status to FAILED here if an entity exists
            chapterEntity?.let {
                chapterDao.updateChapter(it.copy(downloadStatus = DownloadStatus.FAILED))
            }
                return
        }

        val currentStatus = chapterEntity?.downloadStatus
        if (currentStatus == DownloadStatus.DOWNLOADING || currentStatus == DownloadStatus.QUEUED) {
            Log.i(DownloadWorker.TAG, "Chapter ${chapter.id} is already ${currentStatus}. Not re-enqueueing.")
            return
        }
        if (currentStatus == DownloadStatus.DOWNLOADED) {
             Log.i(DownloadWorker.TAG, "Chapter ${chapter.id} is already DOWNLOADED. Not re-enqueueing.")
             // Potentially verify files or offer re-download option, but for now, skip.
            return
        }

        // If entity doesn't exist, or it FAILED/was NOT_DOWNLOADED, create/update it.
        if (chapterEntity == null) {
            chapterEntity = DownloadedChapterEntity(
                chapterId = chapter.id,
                mangaId = manga.id,
                chapterNumber = chapter.chapterNumber,
                title = chapter.title,
                pageCount = chapter.pageCount, // pageCount is now non-null due to check above
                downloadStatus = DownloadStatus.QUEUED // Initial status
            )
            chapterDao.insertChapter(chapterEntity)
            Log.i(DownloadWorker.TAG, "Inserted new chapter into DB for download: ${chapter.title ?: chapter.chapterNumber}")
        } else {
            // Update existing entity to QUEUED, reset downloaded pages, ensure pageCount is correct
            chapterDao.updateChapter(chapterEntity.copy(
                downloadStatus = DownloadStatus.QUEUED,
                downloadedPages = 0, // Reset progress for re-download
                pageCount = chapter.pageCount // Ensure page count is up-to-date
            ))
            Log.i(DownloadWorker.TAG, "Set chapter ${chapter.id} to QUEUED for download/re-download.")
        }

        val data = workDataOf(
            DownloadWorker.KEY_CHAPTER_ID to chapter.id,
            DownloadWorker.KEY_MANGA_ID to manga.id
            // Worker will fetch other details like pageCount from DB based on chapterId
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .addTag("chapter_download_${chapter.id}")
            .build()

        workManager.enqueueUniqueWork(
            "download_${chapter.id}",
            ExistingWorkPolicy.REPLACE, // Replace if exists but not running, useful for retries
            downloadWorkRequest
        )
        Log.i(DownloadWorker.TAG, "Enqueued download work for chapter: ${chapter.title ?: chapter.chapterNumber} of ${manga.title}")
    }

    fun getWorkInfoLiveData(workName: String) = workManager.getWorkInfosForUniqueWorkLiveData(workName)
    fun getWorkInfoByTagLiveData(tag: String) = workManager.getWorkInfosByTagLiveData(tag)

    suspend fun cancelDownload(chapterId: String) {
        val workName = "download_${chapterId}" // Assuming this is the unique work name
        workManager.cancelUniqueWork(workName) // Cancels if running or enqueued
        // Update DB status, e.g., to PAUSED or FAILED depending on desired behavior
        val chapter = chapterDao.getChapterById(chapterId).firstOrNull()
        if (chapter != null && (chapter.downloadStatus == DownloadStatus.DOWNLOADING || chapter.downloadStatus == DownloadStatus.QUEUED)) {
            chapterDao.updateChapterStatus(chapterId, DownloadStatus.PAUSED) // Or NOT_DOWNLOADED or allow user to define
        }
        Log.i(DownloadWorker.TAG, "Attempted to cancel download for chapter: $chapterId")
    }

    suspend fun retryDownload(mangaId: String, chapterId: String) {
        // This might be complex if we need to preserve original MangaInfo/ChapterInfo
        // For simplicity, let's assume we can re-fetch or have enough info in DownloadedChapterEntity/MangaEntity
        val mangaEntity = mangaDao.getMangaById(mangaId).firstOrNull()
        val chapterEntity = chapterDao.getChapterById(chapterId).firstOrNull()

        if (mangaEntity != null && chapterEntity != null &&
            (chapterEntity.downloadStatus == DownloadStatus.FAILED || chapterEntity.downloadStatus == DownloadStatus.PAUSED)) {
            // Convert entities back to domain models (simplified)
            val mangaInfo = MangaInfo(mangaEntity.mangaId, mangaEntity.title, coverImageUrl = mangaEntity.coverImageUrl)
            val chapterInfo = ChapterInfo(chapterEntity.chapterId, chapterEntity.mangaId, chapterEntity.chapterNumber, chapterEntity.title, pageCount = chapterEntity.pageCount)

            Log.i(DownloadWorker.TAG, "Retrying download for chapter: ${chapterInfo.title ?: chapterInfo.chapterNumber}")
            // Enqueue will handle setting status to QUEUED and resetting downloadedPages
            enqueueChapterDownload(mangaInfo, chapterInfo)
        } else {
            Log.w(DownloadWorker.TAG, "Cannot retry chapter $chapterId: Not in FAILED/PAUSED state or entities not found.")
        }
    }

    suspend fun deleteDownloadedChapter(chapterId: String) {
        withContext(Dispatchers.IO) {
            val chapterEntity = chapterDao.getChapterById(chapterId).firstOrNull()
            chapterEntity?.let {
                // 1. Cancel any ongoing work for this chapter
                workManager.cancelUniqueWork("download_${it.chapterId}")

                // 2. Delete files
                it.chapterDownloadPath?.let { path ->
                    val chapterDir = File(path)
                    if (chapterDir.exists()) {
                        if (chapterDir.deleteRecursively()) {
                            Log.i(DownloadWorker.TAG, "Deleted files for chapter $chapterId at $path")
                        } else {
                            Log.e(DownloadWorker.TAG, "Failed to delete files for chapter $chapterId at $path")
                        }
                    }
                }
                // 3. Delete from database
                chapterDao.deleteChapter(it)
                Log.i(DownloadWorker.TAG, "Deleted chapter $chapterId from database.")

                // Optional: Check if manga has any other downloaded chapters, if not, delete manga entry
                val remainingChapters = chapterDao.getChaptersForManga(it.mangaId).firstOrNull()
                if (remainingChapters.isNullOrEmpty()) {
                    mangaDao.getMangaById(it.mangaId).firstOrNull()?.let { manga ->
                        mangaDao.deleteManga(manga)
                        Log.i(DownloadWorker.TAG, "Deleted manga ${manga.mangaId} as no chapters remain.")
                    }
                }
            }
        }
    }
}
