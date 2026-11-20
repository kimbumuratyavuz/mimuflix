package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey
    val mediaId: String,
    val title: String,
    val posterUrl: String?,
    val streamUrl: String,
    val isSeries: Boolean = false,
    val seriesName: String? = null,
    val episodeName: String? = null,
    val positionMs: Long,
    val durationMs: Long,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val progressPercent: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedRemainingTime: String
        get() {
            val remainMs = (durationMs - positionMs).coerceAtLeast(0L)
            val totalSec = remainMs / 1000
            val min = (totalSec / 60) % 60
            val hr = totalSec / 3600
            return if (hr > 0) "${hr} sa ${min} dk kaldı" else "${min} dk kaldı"
        }

    val formattedProgressTime: String
        get() {
            fun fmt(ms: Long): String {
                val sec = (ms / 1000) % 60
                val min = (ms / 60000) % 60
                val hr = ms / 3600000
                return if (hr > 0) String.format("%02d:%02d:%02d", hr, min, sec) else String.format("%02d:%02d", min, sec)
            }
            return "${fmt(positionMs)} / ${fmt(durationMs)}"
        }
}
