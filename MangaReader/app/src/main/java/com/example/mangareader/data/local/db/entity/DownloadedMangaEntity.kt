package com.example.mangareader.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_manga")
data class DownloadedMangaEntity(
    @PrimaryKey val mangaId: String, // Original ID from the source API
    val title: String,
    val coverImageUrl: String?, // Store original URL, cover might be downloaded separately
    val localCoverImagePath: String?, // Path to locally stored cover
    val source: String, // e.g., "MangaDex", "Local"
    val downloadedAt: Long = System.currentTimeMillis()
    // Add other relevant fields from MangaInfo if needed for offline display
)
