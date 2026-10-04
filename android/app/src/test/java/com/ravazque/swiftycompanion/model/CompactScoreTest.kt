package com.ravazque.swiftycompanion.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class CompactScoreTest {
    private val english = Locale.ENGLISH
    private val spanish = Locale.forLanguageTag("es-ES")

    @Test
    fun belowAThousandIsShownAsItIs() {
        assertEquals("0", compactScore(0, english))
        assertEquals("999", compactScore(999, english))
        assertEquals("-999", compactScore(-999, english))
    }

    @Test
    fun fromAThousandIsCutToOneDecimal() {
        assertEquals("1.0k", compactScore(1000, english))
        assertEquals("1.9k", compactScore(1999, english))
        assertEquals("41.3k", compactScore(41337, english))
        assertEquals("-1.0k", compactScore(-1000, english))
        assertEquals("-10.5k", compactScore(-10566, english))
    }

    @Test
    fun theDecimalSeparatorFollowsTheLanguage() {
        assertEquals("41,3k", compactScore(41337, spanish))
        assertEquals("-10,5k", compactScore(-10566, spanish))
    }
}
