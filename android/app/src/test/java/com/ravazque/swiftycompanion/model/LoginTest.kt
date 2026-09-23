package com.ravazque.swiftycompanion.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LoginTest {
    @Test
    fun trimsAndLowercases() {
        assertEquals("jdoe", normalizeLogin("  JDoe "))
        assertEquals("j-doe_2", normalizeLogin("j-doe_2"))
    }

    @Test
    fun rejectsEmptyInput() {
        assertThrows(AppError.EmptyLogin::class.java) { normalizeLogin("   ") }
    }

    @Test
    fun rejectsAnythingThatCouldChangeThePath() {
        listOf("a/b", "../me", "jdoe?x=1", "j doe", "jdoe#", "ñandu").forEach { input ->
            assertThrows(AppError.InvalidLogin::class.java) { normalizeLogin(input) }
        }
    }
}
