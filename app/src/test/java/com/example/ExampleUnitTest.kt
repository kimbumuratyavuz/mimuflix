package com.example

import com.example.data.CatalogRepository
import com.example.model.LanguageBadge
import com.example.model.SeriesSortOrder
import com.example.parser.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testM3uParser_MultipleItemsAndHeaders() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="id1" tvg-name="Film 1" tvg-logo="https://image.tmdb.org/t/p/w500/poster1.jpg" group-title="Filmler", Film 1: Matrix
            #EXTVLCOPT:http-referrer=https://referrer.example.com
            #EXTVLCOPT:http-user-agent=CustomPlayer/2.0
            https://stream.example.com/live/film1.m3u8
            #EXTINF:7200 tvg-name="Dizi Bolum 1" tvg-logo="https://image.tmdb.org/t/p/w500/poster2.jpg", Dizi: Breaking Bad S01E01
            https://stream.example.com/vod/breaking_bad_01.mp4
        """.trimIndent()

        val items = M3uParser.parse(sampleM3u)
        assertEquals(2, items.size)

        val item1 = items[0]
        assertEquals("Film 1: Matrix", item1.name)
        assertEquals("https://stream.example.com/live/film1.m3u8", item1.streamUrl)
        assertEquals("https://image.tmdb.org/t/p/w500/poster1.jpg", item1.logoUrl)
        assertEquals("https://referrer.example.com", item1.httpReferrer)
        assertEquals("CustomPlayer/2.0", item1.httpUserAgent)

        val item2 = items[1]
        assertEquals("Dizi: Breaking Bad S01E01", item2.name)
        assertEquals("https://stream.example.com/vod/breaking_bad_01.mp4", item2.streamUrl)
        assertEquals(7200L, item2.durationSeconds)
    }

    @Test
    fun testCatalogRepository_ParseMovies() {
        val sample = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="https://img.com/poster1.jpg" group-title="Aksiyon | Fantastik ", Street Fighter (2026) - Altyazı
            #EXTVLCOPT:http-referrer=https://twitter.com/
            #EXTVLCOPT:http-user-agent=googleusercontent
            https://stream.com/sf.m3u8
            #EXTINF:-1 tvg-logo="https://img.com/poster2.jpg" group-title="Korku | Gerilim", The Cycle (2026) - Dublaj
            https://stream.com/cycle.m3u8
        """.trimIndent()

        val movies = CatalogRepository.parseMovies(sample)
        assertEquals(2, movies.size)

        val m1 = movies[0]
        assertEquals("Street Fighter (2026) - Altyazı", m1.title)
        assertEquals("Street Fighter (2026)", m1.cleanTitle)
        assertEquals(LanguageBadge.ALTYAZI, m1.language)
        assertEquals(listOf("Aksiyon", "Fantastik"), m1.categories)
        assertEquals("https://img.com/poster1.jpg", m1.posterUrl)
        assertTrue(m1.rawM3uEntry.contains("https://stream.com/sf.m3u8"))

        val m2 = movies[1]
        assertEquals("The Cycle (2026) - Dublaj", m2.title)
        assertEquals("The Cycle (2026)", m2.cleanTitle)
        assertEquals(LanguageBadge.DUBLAJ, m2.language)
        assertEquals(listOf("Korku", "Gerilim"), m2.categories)
    }

    @Test
    fun testCatalogRepository_ParseSeriesAndSort() {
        val sample = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="https://img.com/diziA.jpg" group-title="Dizi A", Dizi A - 1.S Altyazı - Bölüm 1 - Altyazı
            https://stream.com/a1.m3u8
            #EXTINF:-1 tvg-logo="https://img.com/diziA.jpg" group-title="Dizi A", Dizi A - 1.S Altyazı - Bölüm 2 - Altyazı
            https://stream.com/a2.m3u8
            #EXTINF:-1 tvg-logo="https://img.com/diziA.jpg" group-title="Dizi A", Dizi A - 1.S Altyazı - Bölüm 3 - Altyazı
            https://stream.com/a3.m3u8
            #EXTINF:-1 tvg-logo="https://img.com/diziB.jpg" group-title="Dizi B", Dizi B - 1.S Dublaj - Bölüm 1 - Dublaj
            https://stream.com/b1.m3u8
        """.trimIndent()

        val seriesList = CatalogRepository.parseSeries(sample)
        assertEquals(2, seriesList.size)

        val diziA = seriesList.first { it.seriesName == "Dizi A" }
        assertEquals(3, diziA.totalEpisodesCount)
        assertEquals("https://img.com/diziA.jpg", diziA.posterUrl)
        assertTrue(diziA.generatedM3uAllEpisodes.contains("https://stream.com/a1.m3u8"))
        assertTrue(diziA.generatedM3uAllEpisodes.contains("https://stream.com/a3.m3u8"))

        val diziB = seriesList.first { it.seriesName == "Dizi B" }
        assertEquals(1, diziB.totalEpisodesCount)

        // Test sorting: Az bölümden çoğa (ASC)
        val sortedAsc = seriesList.sortedBy { it.totalEpisodesCount }
        assertEquals("Dizi B", sortedAsc[0].seriesName) // 1 episode
        assertEquals("Dizi A", sortedAsc[1].seriesName) // 3 episodes

        // Test sorting: Çok bölümden aza (DESC)
        val sortedDesc = seriesList.sortedByDescending { it.totalEpisodesCount }
        assertEquals("Dizi A", sortedDesc[0].seriesName) // 3 episodes
        assertEquals("Dizi B", sortedDesc[1].seriesName) // 1 episode
    }

    @Test
    fun testSanitizeFileName() {
        val raw = "Film / Dizi: Action * Test ? <Final> | 2024"
        val sanitized = M3uParser.sanitizeFileName(raw)
        assertTrue(!sanitized.contains('/'))
        assertTrue(!sanitized.contains(':'))
        assertTrue(!sanitized.contains('*'))
        assertTrue(!sanitized.contains('?'))
        assertTrue(!sanitized.contains('<'))
        assertTrue(!sanitized.contains('>'))
        assertTrue(!sanitized.contains('|'))
    }
}
