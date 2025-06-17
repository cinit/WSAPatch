package com.example.mangareader.data.local.db.dao

import androidx.room.*
import com.example.mangareader.data.local.db.entity.DownloadedChapterEntity
import com.example.mangareader.data.local.db.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedChapterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: DownloadedChapterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<DownloadedChapterEntity>)

    @Update
    suspend fun updateChapter(chapter: DownloadedChapterEntity)

    @Query("UPDATE downloaded_chapters SET downloadStatus = :status WHERE chapterId = :chapterId")
    suspend fun updateChapterStatus(chapterId: String, status: DownloadStatus)

    @Query("UPDATE downloaded_chapters SET downloadedPages = :pages WHERE chapterId = :chapterId")
    suspend fun updateChapterDownloadedPages(chapterId: String, pages: Int)

    @Delete
    suspend fun deleteChapter(chapter: DownloadedChapterEntity)

    @Query("DELETE FROM downloaded_chapters WHERE mangaId = :mangaId")
    suspend fun deleteChaptersForManga(mangaId: String)

    @Query("SELECT * FROM downloaded_chapters WHERE chapterId = :chapterId")
    fun getChapterById(chapterId: String): Flow<DownloadedChapterEntity?>

    @Query("SELECT * FROM downloaded_chapters WHERE mangaId = :mangaId ORDER BY chapterNumber ASC") // Assuming chapterNumber can be sorted lexicographically or is numeric
    fun getChaptersForManga(mangaId: String): Flow<List<DownloadedChapterEntity>>

    @Query("SELECT * FROM downloaded_chapters WHERE downloadStatus = :status")
    fun getChaptersByStatus(status: DownloadStatus): Flow<List<DownloadedChapterEntity>>

    @Query("SELECT * FROM downloaded_chapters")
    fun getAllDownloadedChapters(): Flow<List<DownloadedChapterEntity>>
}
