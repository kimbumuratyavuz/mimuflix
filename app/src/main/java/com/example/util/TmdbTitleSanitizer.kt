package com.example.util

import java.util.regex.Pattern

object TmdbTitleSanitizer {

    private val YEAR_REGEX = Pattern.compile("\\b(19\\d\\d|20\\d\\d)\\b")

    /**
     * Cleans raw M3U IPTV titles for user-facing display on cards and headers.
     * E.g. "April X (2026) - Dublaj" -> "April X (2026)"
     * E.g. "[TR] Inception (2010) [1080p] [Dual]" -> "Inception (2010)"
     * E.g. "Bytet - 1.S Dublaj" -> "Bytet"
     */
    fun simplifyForDisplay(rawTitle: String): String {
        var t = rawTitle.trim()

        // 1. Remove prefixes like TR:, EN |, [TR], VIP |, FILM |
        t = t.replace(Regex("^(?:\\[[A-Za-z0-9_-]+\\]|[A-Z]{2,5}\\s*[:|]\\s*)+"), "")

        // 2. Remove language suffixes like - Dublaj, - Altyazı, [Dublaj], (Altyazi)
        t = t.replace(Regex("(?i)\\s*-\\s*.*?(?:dublaj|altyazı|altyazi|dual|turkce|türkçe).*?$"), "")
        t = t.replace(Regex("(?i)\\s*\\[[^\\]]*?(?:dublaj|altyazı|altyazi|dual|tr-en|turkce|türkçe)[^\\]]*?\\]"), "")
        t = t.replace(Regex("(?i)\\s*\\([^)]*?(?:dublaj|altyazı|altyazi|dual|tr-en|turkce|türkçe)[^)]*?\\)"), "")

        // 3. Remove quality/resolution tags like [1080p], [4K], FHD, UHD, BluRay, WEB-DL
        t = t.replace(Regex("(?i)\\s*\\[[^\\]]*?(?:1080p|720p|2160p|4k|uhd|fhd|bluray|web-?dl|dvdrip|remux)[^\\]]*?\\]"), "")
        t = t.replace(Regex("(?i)\\s*\\([^)]*?(?:1080p|720p|2160p|4k|uhd|fhd|bluray|web-?dl|dvdrip|remux)[^)]*?\\)"), "")
        t = t.replace(Regex("(?i)\\b(?:1080p|720p|2160p|4k|uhd|fhd|bluray|web-?dl|dvdrip|remux)\\b"), "")

        // 4. Clean season tags in titles like " - 1.S", " 1. Sezon"
        t = t.replace(Regex("(?i)\\s*-\\s*\\d+\\.\\s*s(?:ezon|eason)?.*$"), "")
        t = t.replace(Regex("(?i)\\s*\\b\\d+\\.\\s*s(?:ezon|eason)?\\b.*$"), "")
        t = t.replace(Regex("(?i)\\s*\\bS\\d{1,2}(?:E\\d{1,2})?\\b.*$"), "")

        // Clean extra whitespace and trailing hyphens/colons
        t = t.replace(Regex("\\s+"), " ").trim(' ', '-', ':', '|')
        return t.ifBlank { rawTitle }
    }

    /**
     * Cleans series name for clean display and TMDB search.
     * E.g. "Bytet - 1.S Dublaj" -> "Bytet"
     * E.g. "Stranger Things 1. Sezon" -> "Stranger Things"
     */
    fun simplifySeriesName(seriesName: String): String {
        var s = seriesName.trim()

        // Strip prefix tags
        s = s.replace(Regex("^(?:\\[[A-Za-z0-9_-]+\\]|[A-Z]{2,5}\\s*[:|]\\s*)+"), "")

        // Strip season or episode mentions
        s = s.replace(Regex("(?i)\\s*-\\s*\\d+\\.\\s*s(?:ezon|eason)?.*$"), "")
        s = s.replace(Regex("(?i)\\s*\\b\\d+\\.\\s*s(?:ezon|eason)?\\b.*$"), "")
        s = s.replace(Regex("(?i)\\s*\\bS\\d{1,2}(?:E\\d{1,2})?\\b.*$"), "")
        s = s.replace(Regex("(?i)\\s*-\\s*.*?(?:dublaj|altyazı|altyazi|sezon|season).*?$"), "")
        s = s.replace(Regex("(?i)\\s*\\[[^\\]]*?(?:dublaj|altyazı|altyazi|1080p|4k|sezon|season)[^\\]]*?\\]"), "")
        s = s.replace(Regex("(?i)\\s*\\([^)]*?(?:dublaj|altyazı|altyazi|1080p|4k|sezon|season)[^)]*?\\)"), "")

        // Clean year if standalone at the end
        s = s.replace(Regex("\\s*\\(\\s*(?:19\\d\\d|20\\d\\d)\\s*\\)\\s*$"), "")

        s = s.replace(Regex("\\s+"), " ").trim(' ', '-', ':', '|')
        return s.ifBlank { seriesName }
    }

    /**
     * Extracts pure movie/series query title and optional release year for TMDB search.
     * E.g. "April X (2026) - Dublaj" -> Pair("April X", 2026)
     * E.g. "Noel Baba 3 (2006)" -> Pair("Noel Baba 3", 2006)
     * E.g. "Yırtık Rahibe - Sister Act (1992)" -> Pair("Yırtık Rahibe", 1992)
     */
    fun extractSearchQueryAndYear(rawTitle: String): Pair<String, Int?> {
        val yearMatcher = YEAR_REGEX.matcher(rawTitle)
        var detectedYear: Int? = null
        if (yearMatcher.find()) {
            detectedYear = yearMatcher.group(1)?.toIntOrNull()
        }

        var simplified = simplifyForDisplay(rawTitle)

        // Remove the (YYYY) year from the query string so TMDB search query matches cleanly
        simplified = simplified.replace(Regex("\\s*\\(\\s*(?:19\\d\\d|20\\d\\d)\\s*\\)\\s*"), " ")

        // If title has alternative titles separated by " - ", e.g. "Yırtık Rahibe - Sister Act",
        // we take the first title or provide alternatives.
        val mainTitle = simplified.split(Regex("\\s+-\\s+")).firstOrNull() ?: simplified

        val finalQuery = mainTitle.replace(Regex("\\s+"), " ").trim(' ', '-', ':', '|')
        return Pair(finalQuery.ifBlank { rawTitle }, detectedYear)
    }

    /**
     * Returns fallback/alternative search query if initial query doesn't match.
     * E.g. for "Yırtık Rahibe - Sister Act", returns "Sister Act".
     */
    fun getAlternativeSearchQuery(rawTitle: String): String? {
        val parts = simplifyForDisplay(rawTitle).split(Regex("\\s+-\\s+"))
        if (parts.size > 1) {
            val alt = parts[1].replace(Regex("\\s*\\(\\s*(?:19\\d\\d|20\\d\\d)\\s*\\)\\s*"), " ").trim()
            if (alt.isNotBlank()) return alt
        }
        return null
    }
}
