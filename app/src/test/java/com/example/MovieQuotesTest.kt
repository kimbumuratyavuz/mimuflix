package com.example

import com.example.model.MovieQuotesRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieQuotesTest {

    @Test
    fun `ALL_QUOTES contains exactly 200 quotes`() {
        assertEquals(200, MovieQuotesRepository.ALL_QUOTES.size)
    }

    @Test
    fun `every quote has valid non-blank quote text and movie title`() {
        for ((index, item) in MovieQuotesRepository.ALL_QUOTES.withIndex()) {
            assertFalse("Quote at index $index must not be blank", item.quote.isBlank())
            assertFalse("Movie title at index $index must not be blank", item.movieTitle.isBlank())
            assertTrue("Quote at index $index should be reasonably long", item.quote.length >= 5)
        }
    }

    @Test
    fun `getRandomMovieQuote returns valid quote from pool`() {
        val randomQuote = MovieQuotesRepository.getRandomMovieQuote()
        assertNotNull(randomQuote)
        assertTrue(MovieQuotesRepository.ALL_QUOTES.contains(randomQuote))
    }
}
