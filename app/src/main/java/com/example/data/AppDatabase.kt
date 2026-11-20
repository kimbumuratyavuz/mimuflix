package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.DownloadTask
import com.example.model.FavoriteEntity
import com.example.model.PlaybackProgressEntity

@Database(entities = [DownloadTask::class, FavoriteEntity::class, PlaybackProgressEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun downloadTaskDao(): DownloadTaskDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playbackProgressDao(): PlaybackProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mimu_downloader.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
