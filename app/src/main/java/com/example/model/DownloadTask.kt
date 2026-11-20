package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_tasks")
data class DownloadTask(
    @PrimaryKey
    val id: String,
    val title: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val httpReferrer: String? = null,
    val httpUserAgent: String? = null,
    val targetFileName: String,
    val filePath: String? = null,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val elapsedTimeMs: Long = 0L,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isSeries: Boolean = false,
    val seriesName: String? = null,
    val episodeName: String? = null
)
