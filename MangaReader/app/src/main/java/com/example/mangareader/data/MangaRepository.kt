package com.example.mangareader.data

import com.example.mangareader.data.model.ChapterInfo
import com.example.mangareader.data.model.MangaInfo
import com.example.mangareader.data.remote.mangadex.MangaDexApiService
import com.example.mangareader.data.remote.mangadex.dto.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MangaRepository { // DI would be better (Hilt/Koin)

    private val mangaDexApiService: MangaDexApiService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Log request/response body
        }
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.mangadex.org/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        mangaDexApiService = retrofit.create(MangaDexApiService::class.java)
    }

    // Mapper functions (examples, can be more sophisticated)
    private fun MangaDataDto.toMangaInfo(coverFilename: String? = null, authorName: String? = null, artistName: String? = null): MangaInfo {
        val englishTitle = this.attributes.title["en"] ?: this.attributes.title.values.firstOrNull() ?: this.id
        val englishDescription = this.attributes.description["en"] ?: this.attributes.description.values.firstOrNull()

        var finalCoverUrl: String? = null
        if (coverFilename != null) {
            finalCoverUrl = "https://uploads.mangadex.org/covers/${this.id}/$coverFilename"
        } else {
             this.relationships.find { it.type == "cover_art" }?.id?.let { coverId ->
                // This would require another API call to get filename from coverId, or make getCoverArtDetails suspend
                // For simplicity now, we expect cover filename to be resolved if possible.
             }
        }

        return MangaInfo(
            id = this.id,
            title = englishTitle,
            description = englishDescription,
            coverImageUrl = finalCoverUrl,
            author = authorName ?: this.relationships.find { it.type == "author" }?.attributes?.name ?: "N/A",
            status = this.attributes.status,
            tags = this.attributes.tags.mapNotNull { it.attributes.name["en"] }
        )
    }

    private fun ChapterDataDto.toChapterInfo(mangaId: String): ChapterInfo {
        val scanGroupRel = this.relationships.find { it.type == "scanlation_group" }
        val scanGroupName = scanGroupRel?.attributes?.name ?: scanGroupRel?.id ?: "N/A"

        return ChapterInfo(
            id = this.id,
            mangaId = mangaId,
            chapterNumber = this.attributes.chapter ?: "N/A",
            title = this.attributes.title,
            scanlationGroup = scanGroupName,
            publishAt = this.attributes.publishAt,
            pageCount = this.attributes.pages
        )
    }


    suspend fun getPopularMangaList(limit: Int = 20, offset: Int = 0): Result<List<MangaInfo>> {
        try {
            val response = mangaDexApiService.getMangaList(limit = limit, offset = offset)
            if (response.isSuccessful) {
                val mangaDataList = response.body()?.data ?: emptyList()
                val mangaInfos = mangaDataList.mapNotNull { mangaDto ->
                    val coverArtRelationship = mangaDto.relationships.find { it.type == "cover_art" }
                    val coverFileName = coverArtRelationship?.attributes?.fileName
                    val author = mangaDto.relationships.find { it.type == "author" }?.attributes?.name
                    val artist = mangaDto.relationships.find { it.type == "artist" }?.attributes?.name
                    mangaDto.toMangaInfo(coverFileName, author, artist)
                }
                return Result.success(mangaInfos)
            } else {
                return Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun getMangaDetails(mangaId: String): Result<MangaInfo> {
         try {
            val response = mangaDexApiService.getMangaDetails(mangaId = mangaId)
            if (response.isSuccessful) {
                response.body()?.data?.let { mangaDto ->
                    val coverArtRelationship = mangaDto.relationships.find { it.type == "cover_art" }
                    val coverFileName = coverArtRelationship?.attributes?.fileName
                    val author = mangaDto.relationships.find { it.type == "author" }?.attributes?.name
                    val artist = mangaDto.relationships.find { it.type == "artist" }?.attributes?.name
                    return Result.success(mangaDto.toMangaInfo(coverFileName, author, artist))
                } ?: return Result.failure(Exception("No data found for manga $mangaId"))
            } else {
                return Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun getMangaChapters(mangaId: String, languages: List<String> = listOf("en")): Result<List<ChapterInfo>> {
        try {
            val response = mangaDexApiService.getMangaChapters(mangaId = mangaId, languages = languages)
            if (response.isSuccessful) {
                val chapterDataList = response.body()?.data ?: emptyList()
                val chapterInfos = chapterDataList.map { it.toChapterInfo(mangaId) }
                return Result.success(chapterInfos)
            } else {
                 return Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun getChapterPageImageUrls(chapterId: String): Result<List<String>> {
        try {
            val response = mangaDexApiService.getChapterPages(chapterId)
            if (response.isSuccessful) {
                response.body()?.let { atHomeResponse ->
                    val baseUrl = atHomeResponse.baseUrl
                    val hash = atHomeResponse.chapter.hash
                    val pageFiles = if (atHomeResponse.chapter.pageFilenamesDataSaver.isNotEmpty()) {
                        atHomeResponse.chapter.pageFilenamesDataSaver
                    } else {
                        atHomeResponse.chapter.pageFilenames
                    }
                    val qualityMode = if (atHomeResponse.chapter.pageFilenamesDataSaver.isNotEmpty()) "data-saver" else "data"

                    val fullImageUrls = pageFiles.map { filename ->
                        "$baseUrl/$qualityMode/$hash/$filename"
                    }
                    return Result.success(fullImageUrls)
                } ?: return Result.failure(Exception("No page data found for chapter $chapterId"))
            } else {
                return Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
