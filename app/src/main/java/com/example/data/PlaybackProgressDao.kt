package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.PlaybackProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackProgressDao {

    @Query("SELECT * FROM playback_progress ORDER BY lastUpdated DESC")
    fun getAllProgressFlow(): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress WHERE isSeries = 0 ORDER BY lastUpdated DESC")
    fun getMovieProgressFlow(): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress WHERE isSeries = 1 ORDER BY lastUpdated DESC")
    fun getSeriesProgressFlow(): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getProgressById(mediaId: String): PlaybackProgressEntity?

    @Query("SELECT * FROM playback_progress WHERE streamUrl = :streamUrl LIMIT 1")
    suspend fun getProgressByStreamUrl(streamUrl: String): PlaybackProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: PlaybackProgressEntity)

    @Query("DELETE FROM playback_progress WHERE mediaId = :mediaId")
    suspend fun deleteProgress(mediaId: String)

    @Query("DELETE FROM playback_progress")
    suspend fun clearAll()
}
