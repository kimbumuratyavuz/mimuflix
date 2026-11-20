package com.example.data

import com.example.model.CatalogEpisodeItem
import com.example.model.CatalogMovieItem
import com.example.model.CatalogSeries
import com.example.model.LanguageBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.Reader
import java.io.StringReader
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object CatalogRepository {

    private const val MOVIES_URL = "https://raw.githubusercontent.com/kimbumuratyavuz/capcanli/main/filmuygulama.m3u"
    private const val SERIES_URL = "https://raw.githubusercontent.com/kimbumuratyavuz/capcanli/main/diziuygulama.m3u"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // In-memory cache
    private var cachedMovies: List<CatalogMovieItem>? = null
    private var cachedSeries: List<CatalogSeries>? = null

    fun sanitizePosterUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        var url = rawUrl.trim()

        // Fix malformed concatenated URLs like:
        // "https://m.prectv73.lol/uploads/cache/poster_thumbhttps://a.prectv70.lol/uploads/jpg/..."
        val lastHttps = url.lastIndexOf("https://")
        val lastHttp = url.lastIndexOf("http://")
        val actualStart = maxOf(lastHttps, lastHttp)
        if (actualStart > 0) {
            url = url.substring(actualStart).trim()
        }

        return if (url.startsWith("http://") || url.startsWith("https://")) url else null
    }

    val DEFAULT_FALLBACK_POSTERS = listOf(
        // 100% verified 200 OK Movie Posters
        "https://a.prectv70.lol/uploads/jpg/9d1afa9a78af130e5a7c479868868ca0.jpg",
        "https://a.prectv70.lol/uploads/jpg/719303c5c6e53a2eadc799916f971c24.jpg",
        "https://a.prectv70.lol/uploads/jpg/0cce27ed847b7b2d448bef8d15439fc4.jpg",
        "https://a.prectv70.lol/uploads/jpg/bd46f14f598aa8c65565ea570516e972.jpg",
        "https://a.prectv70.lol/uploads/jpg/3eda123139f8c28d3209906eb5f05332.jpg",
        "https://a.prectv70.lol/uploads/jpg/8483fe9eb3ffa90c0b5da431af2591e1.jpg",
        "https://a.prectv70.lol/uploads/jpg/110f2954272f8684c77fd401abde6ff7.jpg",
        "https://a.prectv70.lol/uploads/jpg/de2f8f204eda5ae6c6ed1c4ebf5afc3f.jpg",
        "https://a.prectv70.lol/uploads/jpg/acd678b3f1471b41ded614fdef064f65.jpg",
        "https://a.prectv70.lol/uploads/jpg/b680132a3c3faeb841dbfa0ea3bc282e.jpg",
        "https://a.prectv70.lol/uploads/jpg/3da824106b596d58f84e979fede7dbb9.jpg",
        "https://a.prectv70.lol/uploads/jpg/f8606fa24857dec6dd1f8952d843a5b6.jpg",
        "https://a.prectv70.lol/uploads/jpg/754ba229d5f6899ba0c52d793e192009.jpg",
        "https://a.prectv70.lol/uploads/jpg/49c877c4368048f6ef8447c79dfe3c91.jpg",
        "https://a.prectv70.lol/uploads/jpg/374b26dcc89cba923862e4d9402a22e4.jpg",
        "https://a.prectv70.lol/uploads/jpg/c868513253581305611fdfa426334890.jpg",
        "https://a.prectv70.lol/uploads/jpg/aa0031b7f526ac84d542ddfca10e0b40.jpg",
        // 100% verified 200 OK Series Posters
        "https://a.prectv70.lol/uploads/cache/poster_thumb/uploads/jpg/9518e27a04325364ea58b998dce06a36.jpg",
        "https://a.prectv70.lol/uploads/cache/poster_thumb/uploads/jpg/f1ab3688c903851e406eba772d4baa33.jpg"
    )

    fun getRandomMediaCover(): String {
        try {
            // 1. In-memory cached movies & series
            val inMemoryMovies = cachedMovies?.mapNotNull { it.posterUrl }?.filter { it.isNotBlank() } ?: emptyList()
            val inMemorySeries = cachedSeries?.mapNotNull { it.posterUrl }?.filter { it.isNotBlank() } ?: emptyList()
            val combined = inMemoryMovies + inMemorySeries
            if (combined.isNotEmpty()) {
                return combined.random()
            }
            // 2. Persistent storage for instant startup
            val prefs = com.example.MimuApplication.instance.getSharedPreferences("mimu_cached_posters", android.content.Context.MODE_PRIVATE)
            val savedSet = prefs.getStringSet("media_posters", null) ?: prefs.getStringSet("movie_posters", null)
            if (!savedSet.isNullOrEmpty()) {
                val cleanSet = savedSet.mapNotNull { sanitizePosterUrl(it) }.filter { it.isNotBlank() }
                if (cleanSet.isNotEmpty()) {
                    return cleanSet.random()
                }
            }
        } catch (e: Exception) {
            // fallback gracefully
        }
        return DEFAULT_FALLBACK_POSTERS.random()
    }

    fun getRandomMovieCover(): String = getRandomMediaCover()

    fun persistMediaPosters(posters: Collection<String?>) {
        try {
            val validUrls = posters.mapNotNull { sanitizePosterUrl(it) }
                .filter { it.isNotBlank() }
                .take(300)
                .toSet()
            if (validUrls.isNotEmpty()) {
                val prefs = com.example.MimuApplication.instance.getSharedPreferences("mimu_cached_posters", android.content.Context.MODE_PRIVATE)
                val existing = prefs.getStringSet("media_posters", null) ?: emptySet()
                val merged = (existing + validUrls).take(500).toSet()
                prefs.edit().putStringSet("media_posters", merged).apply()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private val LOGO_REGEX = Pattern.compile("tvg-logo=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
    private val GROUP_REGEX = Pattern.compile("group-title=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
    private val REFERRER_REGEX = Pattern.compile("http-referrer=([^\\s#]+)", Pattern.CASE_INSENSITIVE)
    private val USER_AGENT_REGEX = Pattern.compile("http-user-agent=([^\\s#]+)", Pattern.CASE_INSENSITIVE)

    suspend fun getMovies(forceRefresh: Boolean = false): List<CatalogMovieItem> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedMovies != null) {
            return@withContext cachedMovies!!
        }

        val request = Request.Builder()
            .url(MOVIES_URL)
            .header("User-Agent", "MimuDownloader/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}: Filmler yüklenemedi")
            val body = response.body ?: throw Exception("Boş veri alındı")
            val movies = parseMoviesFromReader(body.charStream())
            cachedMovies = movies
            persistMediaPosters(movies.mapNotNull { it.posterUrl })
            movies
        }
    }

    suspend fun getSeries(forceRefresh: Boolean = false): List<CatalogSeries> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedSeries != null) {
            return@withContext cachedSeries!!
        }

        val request = Request.Builder()
            .url(SERIES_URL)
            .header("User-Agent", "MimuDownloader/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}: Diziler yüklenemedi")
            val body = response.body ?: throw Exception("Boş veri alındı")
            val series = parseSeriesFromReader(body.charStream())
            cachedSeries = series
            persistMediaPosters(series.mapNotNull { it.posterUrl })
            series
        }
    }

    fun parseMovies(content: String): List<CatalogMovieItem> {
        return parseMoviesFromReader(StringReader(content))
    }

    fun parseSeries(content: String): List<CatalogSeries> {
        return parseSeriesFromReader(StringReader(content))
    }

    fun parseMoviesFromReader(reader: Reader): List<CatalogMovieItem> {
        val items = mutableListOf<CatalogMovieItem>()
        var currentTvgLogo: String? = null
        var currentGroupTitle: String? = null
        var currentRawTitle: String? = null
        var currentReferrer: String? = null
        var currentUserAgent: String? = null

        val bufferedReader = if (reader is BufferedReader) reader else BufferedReader(reader, 16384)
        bufferedReader.forEachLine { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEachLine

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val logoMatcher = LOGO_REGEX.matcher(line)
                currentTvgLogo = if (logoMatcher.find()) sanitizePosterUrl(logoMatcher.group(1)?.trim()) else null

                val groupMatcher = GROUP_REGEX.matcher(line)
                currentGroupTitle = if (groupMatcher.find()) groupMatcher.group(1)?.trim() else null

                val commaIndex = line.lastIndexOf(',')
                currentRawTitle = if (commaIndex != -1 && commaIndex < line.length - 1) {
                    line.substring(commaIndex + 1).trim()
                } else {
                    "Film"
                }
                currentReferrer = null
                currentUserAgent = null
            } else if (line.startsWith("#EXTVLCOPT:", ignoreCase = true)) {
                val refMatcher = REFERRER_REGEX.matcher(line)
                if (refMatcher.find()) currentReferrer = refMatcher.group(1)?.trim()

                val uaMatcher = USER_AGENT_REGEX.matcher(line)
                if (uaMatcher.find()) currentUserAgent = uaMatcher.group(1)?.trim()
            } else if ((line.startsWith("http://", ignoreCase = true) || line.startsWith("https://", ignoreCase = true)) && !line.startsWith("#")) {
                val rawTitle = currentRawTitle ?: "Film"
                val language = detectLanguage(rawTitle)
                val cleanTitle = cleanLanguageSuffix(rawTitle)
                val categories = parseCategories(currentGroupTitle)

                items.add(
                    CatalogMovieItem(
                        id = UUID.nameUUIDFromBytes(line.toByteArray()).toString(),
                        title = rawTitle,
                        cleanTitle = cleanTitle,
                        posterUrl = currentTvgLogo,
                        categories = categories,
                        language = language,
                        streamUrl = line,
                        httpReferrer = currentReferrer,
                        httpUserAgent = currentUserAgent
                    )
                )

                currentTvgLogo = null
                currentGroupTitle = null
                currentRawTitle = null
                currentReferrer = null
                currentUserAgent = null
            }
        }
        return items
    }

    fun parseSeriesFromReader(reader: Reader): List<CatalogSeries> {
        val seriesMap = mutableMapOf<String, MutableList<CatalogEpisodeItem>>()
        val posterMap = mutableMapOf<String, String?>()

        var currentTvgLogo: String? = null
        var currentSeriesName: String? = null
        var currentRawTitle: String? = null
        var currentReferrer: String? = null
        var currentUserAgent: String? = null

        val bufferedReader = if (reader is BufferedReader) reader else BufferedReader(reader, 16384)
        bufferedReader.forEachLine { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEachLine

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                val logoMatcher = LOGO_REGEX.matcher(line)
                currentTvgLogo = if (logoMatcher.find()) sanitizePosterUrl(logoMatcher.group(1)?.trim()) else null

                val groupMatcher = GROUP_REGEX.matcher(line)
                val rawGroup = if (groupMatcher.find()) groupMatcher.group(1)?.trim() else null
                currentSeriesName = rawGroup?.ifBlank { "Diğer Diziler" } ?: "Diğer Diziler"

                val commaIndex = line.lastIndexOf(',')
                currentRawTitle = if (commaIndex != -1 && commaIndex < line.length - 1) {
                    line.substring(commaIndex + 1).trim()
                } else {
                    currentSeriesName
                }
                currentReferrer = null
                currentUserAgent = null
            } else if (line.startsWith("#EXTVLCOPT:", ignoreCase = true)) {
                val refMatcher = REFERRER_REGEX.matcher(line)
                if (refMatcher.find()) currentReferrer = refMatcher.group(1)?.trim()

                val uaMatcher = USER_AGENT_REGEX.matcher(line)
                if (uaMatcher.find()) currentUserAgent = uaMatcher.group(1)?.trim()
            } else if ((line.startsWith("http://", ignoreCase = true) || line.startsWith("https://", ignoreCase = true)) && !line.startsWith("#")) {
                val seriesName = currentSeriesName ?: "Diğer Diziler"
                val rawTitle = currentRawTitle ?: seriesName
                val language = detectLanguage(rawTitle)
                val episodeText = extractEpisodeNumberText(seriesName, rawTitle)
                val seasonNumber = extractSeasonNumber(rawTitle)

                if (!posterMap.containsKey(seriesName) && !currentTvgLogo.isNullOrBlank()) {
                    posterMap[seriesName] = currentTvgLogo
                }

                val list = seriesMap.getOrPut(seriesName) { mutableListOf() }
                list.add(
                    CatalogEpisodeItem(
                        id = UUID.nameUUIDFromBytes(line.toByteArray()).toString(),
                        title = rawTitle,
                        episodeNumberText = episodeText,
                        language = language,
                        streamUrl = line,
                        httpReferrer = currentReferrer,
                        httpUserAgent = currentUserAgent,
                        seasonNumber = seasonNumber
                    )
                )

                currentTvgLogo = null
                currentSeriesName = null
                currentRawTitle = null
                currentReferrer = null
                currentUserAgent = null
            }
        }

        return seriesMap.map { (name, episodes) ->
            val cleanName = com.example.util.TmdbTitleSanitizer.simplifySeriesName(name)
            val languages = episodes.map { it.language }.filter { it != LanguageBadge.ALL }.toSet()
            val finalLanguages = if (languages.isEmpty()) setOf(LanguageBadge.ALL) else languages

            CatalogSeries(
                id = UUID.nameUUIDFromBytes("series:$name".toByteArray()).toString(),
                seriesName = cleanName,
                posterUrl = posterMap[name],
                episodes = episodes,
                languages = finalLanguages,
                totalEpisodesCount = episodes.size
            )
        }
    }

    private fun detectLanguage(title: String): LanguageBadge {
        val lower = title.lowercase()
        val hasDublaj = lower.contains("dublaj")
        val hasAltyazi = lower.contains("altyazı") || lower.contains("altyazi") || lower.contains("alt yazı")

        return when {
            hasDublaj && hasAltyazi -> LanguageBadge.BOTH
            hasDublaj -> LanguageBadge.DUBLAJ
            hasAltyazi -> LanguageBadge.ALTYAZI
            else -> LanguageBadge.ALL
        }
    }

    private fun cleanLanguageSuffix(title: String): String {
        return com.example.util.TmdbTitleSanitizer.simplifyForDisplay(title)
    }

    private fun parseCategories(groupTitle: String?): List<String> {
        if (groupTitle.isNullOrBlank()) return emptyList()
        return groupTitle.split(Regex("[|,/]"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun extractEpisodeNumberText(seriesName: String, rawTitle: String): String {
        val withoutSeries = rawTitle.replace(seriesName, "", ignoreCase = true)
            .replace(Regex("^\\s*-\\s*"), "")
            .trim()
        return withoutSeries.ifBlank { rawTitle }
    }

    fun extractSeasonNumber(rawTitle: String): Int {
        val sAndE = Regex("(?i)\\bS([0-9]{1,2})\\s*E[0-9]{1,2}\\b").find(rawTitle)
        if (sAndE != null) {
            val num = sAndE.groupValues[1].toIntOrNull()
            if (num != null && num > 0) return num
        }
        val dotSezon = Regex("(?i)([0-9]{1,2})\\.\\s*(?:sezon|season|s)\\b").find(rawTitle)
        if (dotSezon != null) {
            val num = dotSezon.groupValues[1].toIntOrNull()
            if (num != null && num > 0) return num
        }
        val seasonWord = Regex("(?i)(?:season|sezon)\\s*([0-9]{1,2})").find(rawTitle)
        if (seasonWord != null) {
            val num = seasonWord.groupValues[1].toIntOrNull()
            if (num != null && num > 0) return num
        }
        val singleS = Regex("(?i)\\bS([0-9]{1,2})\\b").find(rawTitle)
        if (singleS != null) {
            val num = singleS.groupValues[1].toIntOrNull()
            if (num != null && num > 0) return num
        }
        return 1
    }
}
