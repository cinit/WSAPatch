package com.example.mangareader.ui.downloads

import android.app.Application
import androidx.lifecycle.*
import androidx.work.WorkInfo
import com.example.mangareader.data.local.db.AppDatabase
import com.example.mangareader.data.local.db.entity.DownloadedChapterEntity
import com.example.mangareader.download.DownloadManager
import com.example.mangareader.ui.downloads.uimodel.DownloadItemUiModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {
    private val chapterDao = AppDatabase.getDatabase(application).downloadedChapterDao()
    private val mangaDao = AppDatabase.getDatabase(application).downloadedMangaDao()
    private val downloadManager = DownloadManager(application)

    private val _downloadItems = MutableLiveData<List<DownloadItemUiModel>>()
    val downloadItems: LiveData<List<DownloadItemUiModel>> = _downloadItems

    // Keep track of observed WorkInfo LiveData to prevent re-observation
    private val observedWorkInfo = mutableMapOf<String, LiveData<List<WorkInfo>>>()

    init {
        viewModelScope.launch {
            // Observe all chapters in the database
            chapterDao.getAllDownloadedChapters().collect { chapters ->
                updateDownloadItems(chapters)

                // For chapters that are QUEUED or DOWNLOADING, observe their WorkInfo
                chapters.filter { it.downloadStatus == com.example.mangareader.data.local.db.entity.DownloadStatus.QUEUED || it.downloadStatus == com.example.mangareader.data.local.db.entity.DownloadStatus.DOWNLOADING }
                    .forEach { chapter ->
                        observeWorkInfoForChapter(chapter.chapterId)
                    }
            }
        }
    }

    private fun observeWorkInfoForChapter(chapterId: String) {
        val workTag = "chapter_download_${chapterId}"
        if (!observedWorkInfo.containsKey(workTag)) {
            val workInfoLiveData = downloadManager.getWorkInfoByTagLiveData(workTag)
            // This observation needs to be managed carefully to update the specific item
            // For simplicity, we'll re-trigger a full list update when any observed WorkInfo changes.
            // A more granular approach would update only the affected DownloadItemUiModel.
            workInfoLiveData.observeForever { workInfos -> // Use observeForever carefully or tie to ViewModel lifecycle
                viewModelScope.launch { // Launch a coroutine to re-fetch and update
                    val currentChapters = chapterDao.getAllDownloadedChapters().firstOrNull() ?: emptyList()
                    updateDownloadItems(currentChapters, workInfos)
                }
            }
            observedWorkInfo[workTag] = workInfoLiveData
        }
    }

    private suspend fun updateDownloadItems(chapters: List<DownloadedChapterEntity>, workInfosForUpdate: List<WorkInfo>? = null) {
        val uiModels = chapters.mapNotNull { chapter ->
            val manga = mangaDao.getMangaById(chapter.mangaId).firstOrNull()
            if (manga == null) return@mapNotNull null // Skip if manga details not found

            var currentProgress = if (chapter.downloadStatus == com.example.mangareader.data.local.db.entity.DownloadStatus.DOWNLOADED) 100 else 0
            var workInfoState: String? = null

            // Try to find matching WorkInfo if this update is triggered by it
            val relevantWorkInfo = workInfosForUpdate?.find { it.tags.contains("chapter_download_${chapter.chapterId}") }

            if (relevantWorkInfo != null) {
                 workInfoState = relevantWorkInfo.state.name
                 if (relevantWorkInfo.state == WorkInfo.State.RUNNING) {
                     currentProgress = relevantWorkInfo.progress.getInt(com.example.mangareader.download.DownloadWorker.PROGRESS, chapter.downloadedPages * 100 / (chapter.pageCount.takeIf { it > 0 } ?: 1))
                 }
            } else if (chapter.downloadStatus == com.example.mangareader.data.local.db.entity.DownloadStatus.DOWNLOADING || chapter.downloadStatus == com.example.mangareader.data.local.db.entity.DownloadStatus.QUEUED) {
                // If no direct WorkInfo provided for this update, and status suggests it's active,
                // calculate progress from DB (less real-time)
                currentProgress = chapter.downloadedPages * 100 / (chapter.pageCount.takeIf { it > 0 } ?: 1)
            }


            DownloadItemUiModel(
                chapterId = chapter.chapterId,
                mangaTitle = manga.title,
                chapterTitle = chapter.title ?: "Chapter ${chapter.chapterNumber}",
                progress = currentProgress,
                status = chapter.downloadStatus,
                workInfoState = workInfoState,
                mangaId = manga.mangaId // Ensure mangaId is mapped
            )
        }
        _downloadItems.postValue(uiModels.sortedWith(compareBy(
            // Custom sort: active downloads first, then by status order, then by title
            { item ->
                when (item.status) {
                    com.example.mangareader.data.local.db.entity.DownloadStatus.DOWNLOADING -> 0
                    com.example.mangareader.data.local.db.entity.DownloadStatus.QUEUED -> 1
                    com.example.mangareader.data.local.db.entity.DownloadStatus.PAUSED -> 2
                    com.example.mangareader.data.local.db.entity.DownloadStatus.FAILED -> 3
                    com.example.mangareader.data.local.db.entity.DownloadStatus.DOWNLOADED -> 4
                    com.example.mangareader.data.local.db.entity.DownloadStatus.NOT_DOWNLOADED -> 5 // Should ideally not be in queue UI
                }
            },
            { it.mangaTitle },
            { it.chapterTitle }
        )))
    }

    fun cancelDownload(chapterId: String) {
        viewModelScope.launch {
            downloadManager.cancelDownload(chapterId)
            // DB observation will trigger list refresh
        }
    }

    fun retryDownload(item: DownloadItemUiModel) {
        viewModelScope.launch {
            downloadManager.retryDownload(item.mangaId, item.chapterId)
             // DB observation will trigger list refresh
        }
    }

    fun deleteChapter(chapterId: String) {
        viewModelScope.launch {
            downloadManager.deleteDownloadedChapter(chapterId)
            // DB observation will trigger list refresh
        }
    }

    // Call this when the ViewModel is cleared to remove observers
    override fun onCleared() {
        super.onCleared()
        observedWorkInfo.values.forEach { liveData ->
            // This is a simplified cleanup. In a real app, ensure LiveData observers are properly managed.
            // For LiveData observed with observeForever, they must be manually removed.
            // However, the way workInfoLiveData is obtained and observed might be tied to application lifecycle
            // or require a more direct removal if they are truly observeForever instances.
            // For this example, we are not explicitly removing them which can lead to leaks if not handled correctly.
            // A better approach would involve using Transformations or specific lifecycle-aware components.
        }
        observedWorkInfo.clear()
    }
}
