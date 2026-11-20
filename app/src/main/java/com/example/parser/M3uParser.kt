package com.example.parser

import com.example.model.M3uItem
import java.util.UUID
import java.util.regex.Pattern

object M3uParser {

    private val TVG_NAME_REGEX = Pattern.compile("tvg-name=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
    private val TVG_LOGO_REGEX = Pattern.compile("tvg-logo=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
    private val GROUP_TITLE_REGEX = Pattern.compile("group-title=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
    private val TVG_ID_REGEX = Pattern.compile("tvg-id=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE)
    private val DURATION_REGEX = Pattern.compile("#EXTINF:(-?\\d+)")

    fun parse(content: String): List<M3uItem> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return emptyList()

        val lines = trimmed.lines()
        val items = mutableListOf<M3uItem>()

        var currentName: String? = null
        var currentTvgName: String? = null
        var currentLogo: String? = null
        var currentGroup: String? = null
        var currentTvgId: String? = null
        var currentDuration: Long = -1L
        var currentReferrer: String? = null
        var currentUserAgent: String? = null

        var singleUrlCounter = 1

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            when {
                line.startsWith("#EXTM3U", ignoreCase = true) -> {
                    // Header tag, ignore
                }

                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    // Extract duration
                    val durMatcher = DURATION_REGEX.matcher(line)
                    if (durMatcher.find()) {
                        currentDuration = durMatcher.group(1)?.toLongOrNull() ?: -1L
                    }

                    // Extract tvg-name
                    val tvgNameMatcher = TVG_NAME_REGEX.matcher(line)
                    if (tvgNameMatcher.find()) {
                        currentTvgName = tvgNameMatcher.group(1)?.trim()
                    }

                    // Extract tvg-logo
                    val logoMatcher = TVG_LOGO_REGEX.matcher(line)
                    if (logoMatcher.find()) {
                        currentLogo = com.example.data.CatalogRepository.sanitizePosterUrl(logoMatcher.group(1)?.trim())
                    }

                    // Extract group-title
                    val groupMatcher = GROUP_TITLE_REGEX.matcher(line)
                    if (groupMatcher.find()) {
                        currentGroup = groupMatcher.group(1)?.trim()
                    }

                    // Extract tvg-id
                    val idMatcher = TVG_ID_REGEX.matcher(line)
                    if (idMatcher.find()) {
                        currentTvgId = idMatcher.group(1)?.trim()
                    }

                    // Extract title after comma
                    val commaIndex = line.lastIndexOf(',')
                    if (commaIndex != -1 && commaIndex < line.length - 1) {
                        val extracted = line.substring(commaIndex + 1).trim()
                        if (extracted.isNotEmpty()) {
                            currentName = extracted
                        }
                    }
                }

                line.startsWith("#EXTVLCOPT:http-referrer=", ignoreCase = true) -> {
                    currentReferrer = line.substringAfter('=').trim()
                }

                line.startsWith("#EXTVLCOPT:http-user-agent=", ignoreCase = true) -> {
                    currentUserAgent = line.substringAfter('=').trim()
                }

                line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                    val keyVal = line.substringAfter(':').trim()
                    if (keyVal.contains('=')) {
                        val k = keyVal.substringBefore('=').trim().lowercase()
                        val v = keyVal.substringAfter('=').trim()
                        if (k.contains("referrer") || k.contains("referer")) currentReferrer = v
                        if (k.contains("user-agent")) currentUserAgent = v
                    }
                }

                line.startsWith("http://", ignoreCase = true) || line.startsWith("https://", ignoreCase = true) -> {
                    // This is the media URL
                    val streamUrl = line

                    val finalName = currentName
                        ?: currentTvgName
                        ?: extractNameFromUrl(streamUrl, singleUrlCounter)

                    val estimated = estimateSize(currentDuration)

                    items.add(
                        M3uItem(
                            id = UUID.randomUUID().toString(),
                            name = sanitizeTitle(finalName),
                            streamUrl = streamUrl,
                            logoUrl = currentLogo?.takeIf { it.isNotEmpty() },
                            groupTitle = currentGroup?.takeIf { it.isNotEmpty() },
                            tvgName = currentTvgName?.takeIf { it.isNotEmpty() },
                            durationSeconds = currentDuration,
                            estimatedSize = estimated,
                            httpReferrer = currentReferrer,
                            httpUserAgent = currentUserAgent,
                            isSelected = false
                        )
                    )

                    singleUrlCounter++
                    // Reset transient per-entry state
                    currentName = null
                    currentTvgName = null
                    currentLogo = null
                    currentGroup = null
                    currentTvgId = null
                    currentDuration = -1L
                    currentReferrer = null
                    currentUserAgent = null
                }
            }
        }

        // Handle case where user just pasted a direct raw URL without #EXTINF
        if (items.isEmpty() && (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true))) {
            items.add(
                M3uItem(
                    id = UUID.randomUUID().toString(),
                    name = sanitizeTitle(extractNameFromUrl(trimmed, 1)),
                    streamUrl = trimmed,
                    logoUrl = null,
                    groupTitle = null,
                    tvgName = null,
                    durationSeconds = -1L,
                    estimatedSize = "~1.2 GB",
                    httpReferrer = null,
                    httpUserAgent = null,
                    isSelected = false
                )
            )
        }

        return items
    }

    private fun extractNameFromUrl(url: String, index: Int): String {
        return try {
            val path = url.substringBefore('?').substringAfterLast('/')
            val clean = path.substringBeforeLast('.')
            if (clean.isNotBlank() && clean.length > 2) {
                clean.replace('_', ' ').replace('-', ' ').replace("%20", " ")
            } else {
                "Akış $index"
            }
        } catch (_: Exception) {
            "Akış $index"
        }
    }

    private fun estimateSize(durationSeconds: Long): String {
        if (durationSeconds <= 0) return "~1.2 GB"
        val hours = durationSeconds / 3600.0
        val gigabytes = hours * 1.35 // Approx 3 Mbps bitrate
        return if (gigabytes >= 1.0) {
            String.format(java.util.Locale.US, "%.1f GB", gigabytes)
        } else {
            val megabytes = (gigabytes * 1024).toInt()
            "${megabytes.coerceAtLeast(80)} MB"
        }
    }

    fun sanitizeFileName(name: String): String {
        val sanitized = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
        val truncated = if (sanitized.length > 100) sanitized.substring(0, 100).trim() else sanitized
        return truncated.ifEmpty { "Video_" + System.currentTimeMillis() }
    }

    private fun sanitizeTitle(title: String): String {
        return title.replace(Regex("\\s+"), " ").trim()
    }
}
