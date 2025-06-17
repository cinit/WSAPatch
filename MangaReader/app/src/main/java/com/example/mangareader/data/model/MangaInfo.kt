package com.example.mangareader.data.model

data class MangaInfo(
    val id: String,
    val title: String,
    val description: String? = null,
    val coverImageUrl: String? = null,
    val author: String? = null,
    val status: String? = null,
    val tags: List<String>? = null
)
