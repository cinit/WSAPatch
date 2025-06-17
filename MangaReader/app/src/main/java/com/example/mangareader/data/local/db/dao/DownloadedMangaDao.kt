package com.example.mangareader.data.local.db.dao

import androidx.room.*
import com.example.mangareader.data.local.db.entity.DownloadedMangaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedMangaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManga(manga: DownloadedMangaEntity)

    @Update
    suspend fun updateManga(manga: DownloadedMangaEntity)

    @Delete
    suspend fun deleteManga(manga: DownloadedMangaEntity)

    @Query("SELECT * FROM downloaded_manga WHERE mangaId = :mangaId")
    fun getMangaById(mangaId: String): Flow<DownloadedMangaEntity?>

    @Query("SELECT * FROM downloaded_manga ORDER BY title ASC")
    fun getAllDownloadedManga(): Flow<List<DownloadedMangaEntity>>
}
