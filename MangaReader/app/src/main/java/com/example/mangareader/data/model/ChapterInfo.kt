package com.example.mangareader.data.model

data class ChapterInfo(
    val id: String,
    val mangaId: String,
    val chapterNumber: String,
    val title: String? = null,
    val scanlationGroup: String? = null,
    val publishAt: String? = null, // Assuming ISO date string
    val pageCount: Int? = null // Added
)
