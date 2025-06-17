package com.example.mangareader.ui.downloads.uimodel

import com.example.mangareader.data.local.db.entity.DownloadStatus

data class DownloadItemUiModel(
    val chapterId: String,
    val mangaId: String, // Added for retry functionality
    val mangaTitle: String,
    val chapterTitle: String, // e.g., "Chapter 5" or "Vol. 1 Ch. 5: The Adventure"
    val progress: Int, // 0-100
    val status: DownloadStatus,
    val workInfoState: String? = null // From WorkInfo.State (e.g., RUNNING, ENQUEUED) - for debugging/display
)
