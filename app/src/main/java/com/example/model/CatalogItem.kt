package com.example.model

enum class LanguageBadge(val label: String) {
    ALL("Tümü"),
    DUBLAJ("Dublaj"),
    ALTYAZI("Altyazı"),
    BOTH("Dublaj & Altyazı")
}

enum class SeriesSortOrder(val label: String) {
    DEFAULT("Varsayılan"),
    EPISODES_ASC("Az Bölümden Çoğa"),
    EPISODES_DESC("Çok Bölümden Aza"),
    NAME_ASC("İsim (A-Z)")
}

data class CatalogMovieItem(
    val id: String,
    val title: String,
    val cleanTitle: String,
    val posterUrl: String?,
    val categories: List<String>,
    val language: LanguageBadge,
    val streamUrl: String,
    val httpReferrer: String?,
    val httpUserAgent: String?
) {
    val rawM3uEntry: String
        get() {
            val sb = StringBuilder()
            sb.append("#EXTM3U\n")
            sb.append("#EXTINF:-1")
            if (!posterUrl.isNullOrBlank()) sb.append(" tvg-logo=\"$posterUrl\"")
            if (categories.isNotEmpty()) sb.append(" group-title=\"${categories.joinToString(" | ")}\"")
            sb.append(", $title\n")
            if (!httpReferrer.isNullOrBlank()) sb.append("#EXTVLCOPT:http-referrer=$httpReferrer\n")
            if (!httpUserAgent.isNullOrBlank()) sb.append("#EXTVLCOPT:http-user-agent=$httpUserAgent\n")
            sb.append(streamUrl)
            return sb.toString()
        }
}

data class CatalogEpisodeItem(
    val id: String,
    val title: String,
    val episodeNumberText: String,
    val language: LanguageBadge,
    val streamUrl: String,
    val httpReferrer: String?,
    val httpUserAgent: String?,
    val seasonNumber: Int = 1
) {
    val rawM3uEntry: String
        get() {
            val sb = StringBuilder()
            sb.append("#EXTM3U\n")
            sb.append("#EXTINF:-1, $title\n")
            if (!httpReferrer.isNullOrBlank()) sb.append("#EXTVLCOPT:http-referrer=$httpReferrer\n")
            if (!httpUserAgent.isNullOrBlank()) sb.append("#EXTVLCOPT:http-user-agent=$httpUserAgent\n")
            sb.append(streamUrl)
            return sb.toString()
        }
}

data class CatalogSeries(
    val id: String,
    val seriesName: String,
    val posterUrl: String?,
    val episodes: List<CatalogEpisodeItem>,
    val languages: Set<LanguageBadge>,
    val totalEpisodesCount: Int
) {
    val seasons: List<Int>
        get() = episodes.map { it.seasonNumber }.distinct().sorted()

    val totalSeasonsCount: Int
        get() = seasons.size.coerceAtLeast(1)
    val generatedM3uAllEpisodes: String
        get() {
            val sb = StringBuilder()
            sb.append("#EXTM3U\n")
            for (ep in episodes) {
                sb.append("#EXTINF:-1")
                if (!posterUrl.isNullOrBlank()) sb.append(" tvg-logo=\"$posterUrl\"")
                sb.append(" group-title=\"$seriesName\", ${ep.title}\n")
                if (!ep.httpReferrer.isNullOrBlank()) sb.append("#EXTVLCOPT:http-referrer=${ep.httpReferrer}\n")
                if (!ep.httpUserAgent.isNullOrBlank()) sb.append("#EXTVLCOPT:http-user-agent=${ep.httpUserAgent}\n")
                sb.append(ep.streamUrl).append("\n")
            }
            return sb.toString().trimEnd()
        }
}
