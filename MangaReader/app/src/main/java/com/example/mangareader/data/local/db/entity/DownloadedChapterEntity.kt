package com.example.mangareader.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DownloadStatus {
    NOT_DOWNLOADED,
    QUEUED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED,
    PAUSED
}

@Entity(
    tableName = "downloaded_chapters",
    foreignKeys = [ForeignKey(
        entity = DownloadedMangaEntity::class,
        parentColumns = ["mangaId"],
        childColumns = ["mangaId"],
        onDelete = ForeignKey.CASCADE // If a manga is deleted, its chapters are also deleted
    )],
    indices = [Index(value = ["mangaId"]), Index(value = ["chapterId"], unique = true)]
)
data class DownloadedChapterEntity(
    @PrimaryKey val chapterId: String, // Original ID from the source API
    val mangaId: String,
    val chapterNumber: String,
    val title: String?,
    val pageCount: Int, // Total pages in this chapter
    val downloadedPages: Int = 0, // Number of pages successfully downloaded
    var downloadStatus: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    val chapterDownloadPath: String?, // Path to the directory where pages for this chapter are stored
    val downloadedAt: Long = System.currentTimeMillis()
    // We might also store a list of page image file names/paths if needed directly here,
    // or assume a naming convention within chapterDownloadPath
)
