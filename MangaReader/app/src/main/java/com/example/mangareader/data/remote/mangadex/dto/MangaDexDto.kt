package com.example.mangareader.data.remote.mangadex.dto

import com.google.gson.annotations.SerializedName

// Common response wrapper
data class MangaDexResponse<T>(
    val result: String?, // "ok" or "error"
    val response: String?, // "collection" or "entity"
    val data: T,
    val limit: Int?,
    val offset: Int?,
    val total: Int?
)

data class MangaDexListResponse<T>( // For lists like /manga or /manga/{id}/feed
    val result: String?,
    val response: String?,
    val data: List<T>,
    val limit: Int,
    val offset: Int,
    val total: Int
)

// For /at-home/server/{chapterId}
data class AtHomeServerResponse(
    val result: String?, // "ok"
    val baseUrl: String,
    val chapter: ChapterPagesDto
)

data class ChapterPagesDto(
    val hash: String, // Chapter hash
    @SerializedName("data") val pageFilenames: List<String>, // Page filenames
    @SerializedName("dataSaver") val pageFilenamesDataSaver: List<String> // Data saver page filenames
)

// Manga Data Transfer Objects
data class MangaDataDto(
    val id: String,
    val type: String, // "manga"
    val attributes: MangaAttributesDto,
    val relationships: List<RelationshipDto> = emptyList()
)

data class MangaAttributesDto(
    val title: Map<String, String>, // e.g., {"en": "Title"}
    val altTitles: List<Map<String, String>>,
    val description: Map<String, String>,
    val originalLanguage: String,
    val status: String?, // "ongoing", "completed", etc.
    val year: Int?,
    val contentRating: String?,
    val tags: List<TagDto> = emptyList(),
    val createdAt: String?,
    val updatedAt: String?,
    val latestUploadedChapter: String? // This might not be directly available, often from /feed
)

data class TagDto(
    val id: String,
    val type: String, // "tag"
    val attributes: TagAttributesDto
)

data class TagAttributesDto(
    val name: Map<String, String>,
    val group: String // "genre", "theme", "format"
)

data class RelationshipDto(
    val id: String,
    val type: String, // "cover_art", "author", "artist", "chapter", etc.
    val related: String? = null, // For some types like 'manga' for a chapter relationship
    val attributes: GenericAttributesDto? = null // For author/artist names if included directly
)

// Used if author/artist attributes are directly embedded in relationships
data class GenericAttributesDto(
    val name: String? = null, // For author/artist
    val fileName: String? = null // For cover_art
    // Add other potential attributes if needed
)


// Chapter Data Transfer Objects (for /feed endpoint)
data class ChapterDataDto(
    val id: String,
    val type: String, // "chapter"
    val attributes: ChapterAttributesDto,
    val relationships: List<RelationshipDto> = emptyList() // For scanlation_group
)

data class ChapterAttributesDto(
    val volume: String?,
    val chapter: String?,
    val title: String?,
    val translatedLanguage: String,
    val pages: Int, // Number of pages
    val publishAt: String, // Date
    val uploader: String?, // User ID
    val externalUrl: String? // If chapter is hosted externally
)
