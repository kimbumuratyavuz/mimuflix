package com.example

import com.example.data.CatalogRepository
import com.example.model.LanguageBadge
import com.example.model.SeriesSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogParsingTest {

    @Test
    fun `parseMovies successfully extracts movie metadata`() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="https://example.com/matrix.jpg" group-title="Bilim Kurgu | Aksiyon", The Matrix (1999) [TR Dublaj]
            #EXTVLCOPT:http-referrer=https://referrer.example.com
            #EXTVLCOPT:http-user-agent=CustomUA
            https://stream.example.com/matrix.m3u8
            #EXTINF:-1 tvg-logo="https://example.com/godfather.jpg" group-title="Dram", The Godfather [TR Altyazı]
            https://stream.example.com/godfather.m3u8
        """.trimIndent()

        val movies = CatalogRepository.parseMovies(sampleM3u)

        assertEquals(2, movies.size)

        val matrix = movies[0]
        assertEquals("The Matrix (1999) [TR Dublaj]", matrix.title)
        assertEquals("The Matrix (1999)", matrix.cleanTitle)
        assertEquals("https://example.com/matrix.jpg", matrix.posterUrl)
        assertEquals("https://stream.example.com/matrix.m3u8", matrix.streamUrl)
        assertEquals("https://referrer.example.com", matrix.httpReferrer)
        assertEquals("CustomUA", matrix.httpUserAgent)
        assertEquals(LanguageBadge.DUBLAJ, matrix.language)
        assertTrue(matrix.categories.contains("Bilim Kurgu"))
        assertTrue(matrix.categories.contains("Aksiyon"))

        val godfather = movies[1]
        assertEquals("The Godfather", godfather.cleanTitle)
        assertEquals(LanguageBadge.ALTYAZI, godfather.language)
        assertTrue(godfather.categories.contains("Dram"))
    }

    @Test
    fun `parseSeries correctly groups episodes into series container`() {
        val sampleSeriesM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="https://example.com/bb.jpg" group-title="Breaking Bad", Breaking Bad - S01 E01 [TR Dublaj]
            https://stream.example.com/bb_s1e1.m3u8
            #EXTINF:-1 tvg-logo="https://example.com/bb.jpg" group-title="Breaking Bad", Breaking Bad - S01 E02 [TR Dublaj]
            https://stream.example.com/bb_s1e2.m3u8
            #EXTINF:-1 tvg-logo="https://example.com/dark.jpg" group-title="Dark", Dark - 1. Sezon 1. Bölüm [TR Altyazı]
            https://stream.example.com/dark_s1e1.m3u8
        """.trimIndent()

        val seriesList = CatalogRepository.parseSeries(sampleSeriesM3u)

        assertEquals(2, seriesList.size)

        val breakingBad = seriesList.find { it.seriesName == "Breaking Bad" }
        assertNotNull(breakingBad)
        assertEquals(2, breakingBad!!.episodes.size)
        assertEquals("https://example.com/bb.jpg", breakingBad.posterUrl)
        assertTrue(breakingBad.languages.contains(LanguageBadge.DUBLAJ))

        val dark = seriesList.find { it.seriesName == "Dark" }
        assertNotNull(dark)
        assertEquals(1, dark!!.episodes.size)
        assertTrue(dark.languages.contains(LanguageBadge.ALTYAZI))
        assertEquals(1, dark.episodes[0].seasonNumber)
    }

    @Test
    fun `extractSeasonNumber correctly parses diverse season formats`() {
        assertEquals(1, CatalogRepository.extractSeasonNumber("Breaking Bad - S01 E01"))
        assertEquals(2, CatalogRepository.extractSeasonNumber("Stranger Things Season 2 Bölüm 4"))
        assertEquals(3, CatalogRepository.extractSeasonNumber("Dark - 3. Sezon 1. Bölüm"))
        assertEquals(4, CatalogRepository.extractSeasonNumber("Game of Thrones S4E08"))
        assertEquals(5, CatalogRepository.extractSeasonNumber("Friends - Sezon 5 Bölüm 12"))
        assertEquals(2, CatalogRepository.extractSeasonNumber("Lost S02"))
        assertEquals(1, CatalogRepository.extractSeasonNumber("The Boys 1.S 1.Bölüm"))
        assertEquals(2, CatalogRepository.extractSeasonNumber("The Boys 2.S 3.Bölüm"))
        assertEquals(3, CatalogRepository.extractSeasonNumber("Fringe 3. S. 5. Bölüm"))
        assertEquals(4, CatalogRepository.extractSeasonNumber("Dexter 4.s"))
        assertEquals(1, CatalogRepository.extractSeasonNumber("Rastgele Dizi Başlığı"))
    }

    @Test
    fun `sanitizePosterUrl correctly fixes malformed concatenated poster URLs`() {
        val malformed1 = "https://m.prectv73.lol/uploads/cache/poster_thumbhttps://a.prectv70.lol/uploads/jpg/47efb87455c66b21485ce256c332d6d8.jpg"
        assertEquals("https://a.prectv70.lol/uploads/jpg/47efb87455c66b21485ce256c332d6d8.jpg", CatalogRepository.sanitizePosterUrl(malformed1))

        val malformed2 = "https://a.prectv70.lol/uploads/cache/poster_thumbhttps://a.prectv70.lol/uploads/jpg/b680132a3c3faeb841dbfa0ea3bc282e.jpg"
        assertEquals("https://a.prectv70.lol/uploads/jpg/b680132a3c3faeb841dbfa0ea3bc282e.jpg", CatalogRepository.sanitizePosterUrl(malformed2))

        val normal = "https://a.prectv70.lol/uploads/jpg/normal.jpg"
        assertEquals("https://a.prectv70.lol/uploads/jpg/normal.jpg", CatalogRepository.sanitizePosterUrl(normal))

        assertEquals(null, CatalogRepository.sanitizePosterUrl(null))
        assertEquals(null, CatalogRepository.sanitizePosterUrl(""))
    }

    @Test
    fun `parseSeries produces deterministic series IDs across repeated parses`() {
        val sampleSeriesM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="https://example.com/bb.jpg" group-title="Breaking Bad", Breaking Bad - S01 E01 [TR Dublaj]
            https://stream.example.com/bb_s1e1.m3u8
            #EXTINF:-1 tvg-logo="https://example.com/dark.jpg" group-title="Dark", Dark - 1. Sezon 1. Bölüm [TR Altyazı]
            https://stream.example.com/dark_s1e1.m3u8
        """.trimIndent()

        val firstParse = CatalogRepository.parseSeries(sampleSeriesM3u)
        val secondParse = CatalogRepository.parseSeries(sampleSeriesM3u)

        assertEquals(firstParse.map { it.id }, secondParse.map { it.id })
    }
}
