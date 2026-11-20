package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.DownloadStatus
import com.example.model.DownloadTask
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadTaskDao {

    @Query("SELECT * FROM download_tasks ORDER BY createdAt ASC")
    fun getAllTasks(): Flow<List<DownloadTask>>

    @Query("SELECT * FROM download_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): DownloadTask?

    @Query("SELECT * FROM download_tasks WHERE status = :status ORDER BY createdAt ASC")
    suspend fun getTasksByStatus(status: DownloadStatus): List<DownloadTask>

    @Query("SELECT * FROM download_tasks WHERE status IN (:statuses) ORDER BY createdAt ASC")
    suspend fun getTasksByStatuses(statuses: List<DownloadStatus>): List<DownloadTask>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: DownloadTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<DownloadTask>)

    @Update
    suspend fun update(task: DownloadTask)

    @Delete
    suspend fun delete(task: DownloadTask)

    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM download_tasks")
    suspend fun deleteAll()
}
