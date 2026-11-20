package com.example.model

import java.io.File

data class DownloadedMovie(
    val file: File,
    val title: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val correspondingTask: DownloadTask? = null,
    val posterUrl: String? = null
)

data class DownloadedEpisode(
    val file: File,
    val episodeName: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val correspondingTask: DownloadTask? = null,
    val posterUrl: String? = null
)

data class DownloadedSeriesFolder(
    val seriesName: String,
    val folder: File,
    val episodes: List<DownloadedEpisode>,
    val totalSizeBytes: Long,
    val posterUrl: String? = null
)
