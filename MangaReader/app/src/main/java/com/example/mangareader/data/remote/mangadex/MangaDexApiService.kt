package com.example.mangareader.data.remote.mangadex

import com.example.mangareader.data.remote.mangadex.dto.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Response // For more control over API responses

interface MangaDexApiService { // Renamed from MangaApiService for clarity
    @GET("manga")
    suspend fun getMangaList(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("title") title: String? = null, // For search
        @Query("order[latestUploadedChapter]") orderLatest: String = "desc", // Example order
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author", "artist")
    ): Response<MangaDexListResponse<MangaDataDto>>

    @GET("manga/{id}")
    suspend fun getMangaDetails(
        @Path("id") mangaId: String,
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author", "artist")
    ): Response<MangaDexResponse<MangaDataDto>> // Single manga uses MangaDexResponse

    @GET("manga/{id}/feed")
    suspend fun getMangaChapters(
        @Path("id") mangaId: String,
        @Query("limit") limit: Int = 100, // Adjust as needed, API max is 500
        @Query("offset") offset: Int = 0,
        @Query("translatedLanguage[]") languages: List<String> = listOf("en"), // Default to English
        @Query("order[volume]") volumeOrder: String = "asc",
        @Query("order[chapter]") chapterOrder: String = "asc",
        @Query("includes[]") includes: List<String> = listOf("scanlation_group") // To get group name
    ): Response<MangaDexListResponse<ChapterDataDto>>

    @GET("at-home/server/{chapterId}")
    suspend fun getChapterPages(
        @Path("chapterId") chapterId: String
    ): Response<AtHomeServerResponse>

    // Endpoint to get Cover Art details (filename)
    // Not strictly needed if we construct URL from MangaDataDto relationship, but good for completeness
    @GET("cover/{coverId}")
    suspend fun getCoverArtDetails(@Path("coverId") coverId: String): Response<MangaDexResponse<RelationshipDto>>
}
