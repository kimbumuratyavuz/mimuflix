package com.example.model

import java.util.Locale

data class TmdbDetail(
    val tmdbId: Int,
    val title: String,
    val originalTitle: String? = null,
    val overview: String? = null,
    val releaseYear: String? = null,
    val voteAverage: Double? = null,
    val voteCount: Int = 0,
    val genres: List<String> = emptyList(),
    val director: String? = null,
    val cast: List<TmdbCastMember> = emptyList(),
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val runtimeMinutes: Int? = null,
    val isTvSeries: Boolean = false
) {
    val formattedRating: String?
        get() = voteAverage?.let {
            if (it > 0.0) String.format(Locale.US, "%.1f", it) else null
        }

    val formattedDuration: String?
        get() = runtimeMinutes?.takeIf { it > 0 }?.let { mins ->
            val hours = mins / 60
            val remainder = mins % 60
            if (hours > 0) "${hours} sa ${remainder} dk" else "${mins} dk"
        }
}

data class TmdbCastMember(
    val id: Int,
    val name: String,
    val character: String? = null,
    val profileUrl: String? = null
)

sealed interface TmdbUiState {
    object Idle : TmdbUiState
    object Loading : TmdbUiState
    data class Success(val detail: TmdbDetail) : TmdbUiState
    data class Error(val message: String) : TmdbUiState
}
