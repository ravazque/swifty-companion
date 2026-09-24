package com.ravazque.swiftycompanion.data

import com.ravazque.swiftycompanion.data.net.CoalitionDto
import com.ravazque.swiftycompanion.data.net.IntraJson
import com.ravazque.swiftycompanion.data.net.UserDto
import com.ravazque.swiftycompanion.model.ProjectStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class ProfileMapperTest {
    private val fixture = javaClass.classLoader!!.getResource("user.json")!!.readText()
    private val profile = IntraJson.decodeFromString<UserDto>(fixture).toProfile(CoalitionDto("Blue", "#3F8EFC", null))

    @Test
    fun mapsIdentityAndDetails() {
        assertEquals("Johnny Doe", profile.displayName)
        assertEquals("https://cdn.example.com/users/medium_jdoe.jpg", profile.imageUrl)
        assertNull(profile.phone)
        assertEquals("c1r2s3", profile.location)
        assertEquals(YearMonth.of(2024, 7), profile.pool)
        assertEquals("South", profile.campus)
        assertEquals("Mastermind jdoe", profile.title)
        assertEquals("Blue", profile.coalition?.name)
    }

    @Test
    fun mainCursusComesFirst() {
        val main = profile.mainCursus!!
        assertEquals("42cursus", main.slug)
        assertEquals(14, main.levelNumber)
        assertEquals(19, main.levelPercent)
        assertEquals(listOf("42cursus", "c-piscine"), profile.cursus.map { it.slug })
    }

    @Test
    fun cursusSelectionFallsBackToTheMainOne() {
        assertEquals("c-piscine", profile.cursusOrMain(9)?.slug)
        assertEquals("42cursus", profile.cursusOrMain(999)?.slug)
        assertEquals("42cursus", profile.cursusOrMain(null)?.slug)
    }

    @Test
    fun skillPercentagesUseTheChartScale() {
        val skills = profile.mainCursus!!.skills
        assertEquals("Unix", skills.first().name)
        assertEquals(100.0, skills.first().percent, 1e-9)
        assertEquals(50.0, skills.last().percent, 1e-9)
    }

    @Test
    fun projectStatusFollowsStatusThenValidation() {
        val byName = profile.projects.associate { it.name to it.status }
        assertEquals(ProjectStatus.VALIDATED, byName["Libft"])
        assertEquals(ProjectStatus.FAILED, byName["Printf"])
        assertEquals(ProjectStatus.IN_PROGRESS, byName["Shell"])
        assertEquals(ProjectStatus.IN_PROGRESS, byName["Pipex"])
    }

    @Test
    fun sparseAccountFallsBackToDefaults() {
        val staff = IntraJson.decodeFromString<UserDto>("""{"id": 2, "login": "boss", "staff?": true, "wallet": null}""")
            .toProfile(null)

        assertEquals("boss", staff.displayName)
        assertTrue(staff.isStaff)
        assertEquals(0, staff.wallet)
        assertNull(staff.mainCursus)
        assertTrue(staff.projects.isEmpty())
        assertNull(staff.title)
    }
}
