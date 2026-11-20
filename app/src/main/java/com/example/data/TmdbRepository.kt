package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.model.TmdbCastMember
import com.example.model.TmdbDetail
import com.example.util.TmdbTitleSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object TmdbRepository {

    private const val TAG = "TmdbRepository"
    private const val BASE_URL = "https://api.themoviedb.org/3"
    private const val POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500"
    private const val BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/w1280"
    private const val PROFILE_BASE_URL = "https://image.tmdb.org/t/p/w185"

    // Default key supplied by user
    private const val FALLBACK_API_KEY = "04e8bb37c5e5a6f2114ed492b53fac15"

    private fun getApiKey(): String {
        return try {
            val key = BuildConfig.TMDB_API_KEY
            if (key.isNullOrBlank() || key.contains("YOUR_")) FALLBACK_API_KEY else key
        } catch (e: Throwable) {
            FALLBACK_API_KEY
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // In-memory cache to avoid duplicate API calls
    private val movieCache = ConcurrentHashMap<String, TmdbDetail>()
    private val seriesCache = ConcurrentHashMap<String, TmdbDetail>()

    /**
     * Fetches detailed TMDB metadata for a movie by raw or clean title.
     */
    suspend fun getMovieDetail(rawTitle: String): TmdbDetail? = withContext(Dispatchers.IO) {
        val cacheKey = rawTitle.trim().lowercase()
        movieCache[cacheKey]?.let { return@withContext it }

        val (query, detectedYear) = TmdbTitleSanitizer.extractSearchQueryAndYear(rawTitle)
        val apiKey = getApiKey()

        try {
            // 1. Search movie with year if available
            var movieId = searchMovieId(query, detectedYear, apiKey)

            // If not found and year was provided, retry without year
            if (movieId == null && detectedYear != null) {
                movieId = searchMovieId(query, null, apiKey)
            }

            // If still not found, try alternative title (e.g. English part of Turkish - English title)
            if (movieId == null) {
                val altQuery = TmdbTitleSanitizer.getAlternativeSearchQuery(rawTitle)
                if (altQuery != null) {
                    movieId = searchMovieId(altQuery, detectedYear, apiKey) ?: searchMovieId(altQuery, null, apiKey)
                }
            }

            if (movieId == null) {
                Log.d(TAG, "No TMDB movie found for: $rawTitle (query: $query)")
                return@withContext null
            }

            // 2. Fetch full details with credits
            val detail = fetchMovieDetails(movieId, apiKey)
            if (detail != null) {
                movieCache[cacheKey] = detail
            }
            detail
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching TMDB movie detail for $rawTitle: ${e.message}", e)
            null
        }
    }

    /**
     * Fetches detailed TMDB metadata for a series by series name.
     */
    suspend fun getSeriesDetail(rawSeriesName: String): TmdbDetail? = withContext(Dispatchers.IO) {
        val cleanName = TmdbTitleSanitizer.simplifySeriesName(rawSeriesName)
        val cacheKey = cleanName.lowercase()
        seriesCache[cacheKey]?.let { return@withContext it }

        val apiKey = getApiKey()

        try {
            // 1. Search TV series
            var tvId = searchSeriesId(cleanName, apiKey)

            if (tvId == null) {
                val altQuery = TmdbTitleSanitizer.getAlternativeSearchQuery(rawSeriesName)
                if (altQuery != null) {
                    tvId = searchSeriesId(altQuery, apiKey)
                }
            }

            if (tvId == null) {
                Log.d(TAG, "No TMDB TV series found for: $rawSeriesName (query: $cleanName)")
                return@withContext null
            }

            // 2. Fetch full series details with credits
            val detail = fetchSeriesDetails(tvId, apiKey)
            if (detail != null) {
                seriesCache[cacheKey] = detail
            }
            detail
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching TMDB series detail for $rawSeriesName: ${e.message}", e)
            null
        }
    }

    private fun searchMovieId(query: String, year: Int?, apiKey: String): Int? {
        val urlBuilder = "$BASE_URL/search/movie".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("api_key", apiKey)
            ?.addQueryParameter("query", query)
            ?.addQueryParameter("language", "tr-TR")
            ?: return null

        if (year != null && year in 1900..2040) {
            urlBuilder.addQueryParameter("year", year.toString())
        }

        val request = Request.Builder()
            .url(urlBuilder.build())
            .header("User-Agent", "MimuFlix/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return null
            if (results.length() > 0) {
                val first = results.getJSONObject(0)
                return first.optInt("id", -1).takeIf { it > 0 }
            }
        }
        return null
    }

    private fun searchSeriesId(query: String, apiKey: String): Int? {
        val url = "$BASE_URL/search/tv".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("api_key", apiKey)
            ?.addQueryParameter("query", query)
            ?.addQueryParameter("language", "tr-TR")
            ?.build() ?: return null

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MimuFlix/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return null
            if (results.length() > 0) {
                val first = results.getJSONObject(0)
                return first.optInt("id", -1).takeIf { it > 0 }
            }
        }
        return null
    }

    private fun fetchMovieDetails(movieId: Int, apiKey: String): TmdbDetail? {
        val url = "$BASE_URL/movie/$movieId?api_key=$apiKey&language=tr-TR&append_to_response=credits"
        val request = Request.Builder().url(url).header("User-Agent", "MimuFlix/1.0").build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)

            var overview = json.optString("overview", "").trim()

            // If Turkish overview is empty, fallback to English overview
            if (overview.isBlank()) {
                val enUrl = "$BASE_URL/movie/$movieId?api_key=$apiKey&language=en-US"
                try {
                    val enReq = Request.Builder().url(enUrl).build()
                    client.newCall(enReq).execute().use { enResp ->
                        if (enResp.isSuccessful) {
                            val enJson = JSONObject(enResp.body?.string() ?: "")
                            val enOverview = enJson.optString("overview", "").trim()
                            if (enOverview.isNotBlank()) {
                                overview = enOverview
                            }
                        }
                    }
                } catch (e: Exception) {
                    // ignore fallback failure
                }
            }

            val title = json.optString("title").ifBlank { json.optString("original_title") }
            val originalTitle = json.optString("original_title", "").takeIf { it.isNotBlank() }
            val releaseDate = json.optString("release_date", "")
            val releaseYear = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else null
            val voteAverage = json.optDouble("vote_average", 0.0).takeIf { it > 0.0 }
            val voteCount = json.optInt("vote_count", 0)
            val runtime = json.optInt("runtime", 0).takeIf { it > 0 }

            val posterPath = json.optString("poster_path", "").takeIf { it.isNotBlank() }
            val backdropPath = json.optString("backdrop_path", "").takeIf { it.isNotBlank() }

            // Genres
            val genres = mutableListOf<String>()
            val genresArray = json.optJSONArray("genres")
            if (genresArray != null) {
                for (i in 0 until genresArray.length()) {
                    val g = genresArray.getJSONObject(i).optString("name")
                    if (g.isNotBlank()) genres.add(g)
                }
            }

            // Credits (Director & Cast)
            var director: String? = null
            val castList = mutableListOf<TmdbCastMember>()

            val credits = json.optJSONObject("credits")
            if (credits != null) {
                // Find Director in crew
                val crewArray = credits.optJSONArray("crew")
                if (crewArray != null) {
                    for (i in 0 until crewArray.length()) {
                        val member = crewArray.getJSONObject(i)
                        val job = member.optString("job")
                        if (job.equals("Director", ignoreCase = true)) {
                            director = member.optString("name")
                            break
                        }
                    }
                }

                // First 8 Cast Members
                val castArray = credits.optJSONArray("cast")
                if (castArray != null) {
                    val limit = castArray.length().coerceAtMost(8)
                    for (i in 0 until limit) {
                        val c = castArray.getJSONObject(i)
                        val id = c.optInt("id")
                        val name = c.optString("name")
                        val character = c.optString("character", "").takeIf { it.isNotBlank() }
                        val profilePath = c.optString("profile_path", "").takeIf { it.isNotBlank() }
                        if (name.isNotBlank()) {
                            castList.add(
                                TmdbCastMember(
                                    id = id,
                                    name = name,
                                    character = character,
                                    profileUrl = profilePath?.let { "$PROFILE_BASE_URL$it" }
                                )
                            )
                        }
                    }
                }
            }

            return TmdbDetail(
                tmdbId = movieId,
                title = title,
                originalTitle = originalTitle,
                overview = overview.takeIf { it.isNotBlank() },
                releaseYear = releaseYear,
                voteAverage = voteAverage,
                voteCount = voteCount,
                genres = genres,
                director = director,
                cast = castList,
                posterUrl = posterPath?.let { "$POSTER_BASE_URL$it" },
                backdropUrl = backdropPath?.let { "$BACKDROP_BASE_URL$it" },
                runtimeMinutes = runtime,
                isTvSeries = false
            )
        }
    }

    private fun fetchSeriesDetails(tvId: Int, apiKey: String): TmdbDetail? {
        val url = "$BASE_URL/tv/$tvId?api_key=$apiKey&language=tr-TR&append_to_response=credits"
        val request = Request.Builder().url(url).header("User-Agent", "MimuFlix/1.0").build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)

            var overview = json.optString("overview", "").trim()

            // If Turkish overview is empty, fallback to English overview
            if (overview.isBlank()) {
                val enUrl = "$BASE_URL/tv/$tvId?api_key=$apiKey&language=en-US"
                try {
                    val enReq = Request.Builder().url(enUrl).build()
                    client.newCall(enReq).execute().use { enResp ->
                        if (enResp.isSuccessful) {
                            val enJson = JSONObject(enResp.body?.string() ?: "")
                            val enOverview = enJson.optString("overview", "").trim()
                            if (enOverview.isNotBlank()) {
                                overview = enOverview
                            }
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            val title = json.optString("name").ifBlank { json.optString("original_name") }
            val originalTitle = json.optString("original_name", "").takeIf { it.isNotBlank() }
            val firstAirDate = json.optString("first_air_date", "")
            val releaseYear = if (firstAirDate.length >= 4) firstAirDate.substring(0, 4) else null
            val voteAverage = json.optDouble("vote_average", 0.0).takeIf { it > 0.0 }
            val voteCount = json.optInt("vote_count", 0)

            val posterPath = json.optString("poster_path", "").takeIf { it.isNotBlank() }
            val backdropPath = json.optString("backdrop_path", "").takeIf { it.isNotBlank() }

            // Genres
            val genres = mutableListOf<String>()
            val genresArray = json.optJSONArray("genres")
            if (genresArray != null) {
                for (i in 0 until genresArray.length()) {
                    val g = genresArray.getJSONObject(i).optString("name")
                    if (g.isNotBlank()) genres.add(g)
                }
            }

            // Creator / Director
            var creatorOrDirector: String? = null
            val createdByArray = json.optJSONArray("created_by")
            if (createdByArray != null && createdByArray.length() > 0) {
                creatorOrDirector = createdByArray.getJSONObject(0).optString("name")
            }

            val credits = json.optJSONObject("credits")
            if (creatorOrDirector == null && credits != null) {
                val crewArray = credits.optJSONArray("crew")
                if (crewArray != null) {
                    for (i in 0 until crewArray.length()) {
                        val m = crewArray.getJSONObject(i)
                        val job = m.optString("job")
                        if (job.equals("Director", ignoreCase = true) || job.equals("Executive Producer", ignoreCase = true)) {
                            creatorOrDirector = m.optString("name")
                            break
                        }
                    }
                }
            }

            // First 8 Cast Members
            val castList = mutableListOf<TmdbCastMember>()
            if (credits != null) {
                val castArray = credits.optJSONArray("cast")
                if (castArray != null) {
                    val limit = castArray.length().coerceAtMost(8)
                    for (i in 0 until limit) {
                        val c = castArray.getJSONObject(i)
                        val id = c.optInt("id")
                        val name = c.optString("name")
                        val character = c.optString("character", "").takeIf { it.isNotBlank() }
                        val profilePath = c.optString("profile_path", "").takeIf { it.isNotBlank() }
                        if (name.isNotBlank()) {
                            castList.add(
                                TmdbCastMember(
                                    id = id,
                                    name = name,
                                    character = character,
                                    profileUrl = profilePath?.let { "$PROFILE_BASE_URL$it" }
                                )
                            )
                        }
                    }
                }
            }

            return TmdbDetail(
                tmdbId = tvId,
                title = title,
                originalTitle = originalTitle,
                overview = overview.takeIf { it.isNotBlank() },
                releaseYear = releaseYear,
                voteAverage = voteAverage,
                voteCount = voteCount,
                genres = genres,
                director = creatorOrDirector,
                cast = castList,
                posterUrl = posterPath?.let { "$POSTER_BASE_URL$it" },
                backdropUrl = backdropPath?.let { "$BACKDROP_BASE_URL$it" },
                runtimeMinutes = null,
                isTvSeries = true
            )
        }
    }
}
