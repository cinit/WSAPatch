package com.example.mangareader.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mangareader.data.local.db.dao.DownloadedChapterDao
import com.example.mangareader.data.local.db.dao.DownloadedMangaDao
import com.example.mangareader.data.local.db.entity.DownloadedChapterEntity
import com.example.mangareader.data.local.db.entity.DownloadedMangaEntity
import com.example.mangareader.data.local.db.entity.DownloadStatus


// Type converter for DownloadStatus enum
class DownloadStatusConverter {
    @androidx.room.TypeConverter
    fun toDownloadStatus(value: String) = enumValueOf<DownloadStatus>(value)

    @androidx.room.TypeConverter
    fun fromDownloadStatus(value: DownloadStatus) = value.name
}


@Database(entities = [DownloadedMangaEntity::class, DownloadedChapterEntity::class], version = 1, exportSchema = false)
@TypeConverters(DownloadStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadedMangaDao(): DownloadedMangaDao
    abstract fun downloadedChapterDao(): DownloadedChapterDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "manga_reader_database"
                )
                // Wipes and rebuilds instead of migrating if no Migration object.
                // Migration is not part of this subtask.
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
