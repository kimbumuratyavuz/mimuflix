package com.example.model

data class PlayerMediaItem(
    val title: String,
    val mediaUri: String, // local file path or http/https stream url
    val isStream: Boolean = false,
    val isSeries: Boolean = false,
    val seriesName: String? = null,
    val episodeName: String? = null,
    val playlist: List<PlayerMediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val initialPositionMs: Long = 0L,
    val mediaId: String = "",
    val posterUrl: String? = null
) {
    val hasNextEpisode: Boolean
        get() = playlist.isNotEmpty() && currentIndex in 0 until (playlist.size - 1)

    val hasPreviousEpisode: Boolean
        get() = playlist.isNotEmpty() && currentIndex > 0

    fun getNextEpisode(): PlayerMediaItem? {
        return if (hasNextEpisode) {
            playlist[currentIndex + 1].copy(
                playlist = playlist,
                currentIndex = currentIndex + 1
            )
        } else null
    }

    fun getPreviousEpisode(): PlayerMediaItem? {
        return if (hasPreviousEpisode) {
            playlist[currentIndex - 1].copy(
                playlist = playlist,
                currentIndex = currentIndex - 1
            )
        } else null
    }
}
