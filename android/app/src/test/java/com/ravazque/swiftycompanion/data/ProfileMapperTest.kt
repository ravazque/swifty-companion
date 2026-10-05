package com.ravazque.swiftycompanion.data

import com.ravazque.swiftycompanion.data.net.CoalitionDto
import com.ravazque.swiftycompanion.data.net.IntraJson
import com.ravazque.swiftycompanion.data.net.UserDto
import com.ravazque.swiftycompanion.model.ProfileKind
import com.ravazque.swiftycompanion.model.ProjectStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class ProfileMapperTest {
    private val now = Instant.parse("2026-10-04T00:00:00Z")
    private val fixture = javaClass.classLoader!!.getResource("user.json")!!.readText()
    private val profile = IntraJson.decodeFromString<UserDto>(fixture).toProfile(now)

    @Test
    fun mapsIdentityAndDetails() {
        assertEquals("Johnny Doe", profile.displayName)
        assertEquals("https://cdn.example.com/users/medium_jdoe.jpg", profile.imageUrl)
        assertEquals("c1r2s3", profile.location)
        assertEquals(YearMonth.of(2024, 7), profile.pool)
        assertEquals("Mastermind jdoe", profile.title)
        assertEquals(ProfileKind.TRANSCENDER, profile.kind)
        assertNull(profile.coalition)
    }

    @Test
    fun mainCursusComesFirstWithItsDates() {
        val main = profile.mainCursus!!
        assertEquals("42cursus", main.slug)
        assertEquals(14, main.levelNumber)
        assertEquals(19, main.levelPercent)
        assertEquals(Instant.parse("2024-10-01T07:00:00Z"), main.beginAt)
        assertNull(main.endAt)
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
    fun openAttemptsKeepTheirStatusAndTheRestFollowTheLatestGrade() {
        val byName = profile.projects.associate { it.name to it.status }
        assertEquals(ProjectStatus.PASSED, byName["Libft"])
        assertEquals(ProjectStatus.FAILED, byName["Printf"])
        assertEquals(ProjectStatus.IN_PROGRESS, byName["Shell"])
        assertEquals(ProjectStatus.WAITING_FOR_CORRECTION, byName["Pipex"])
        assertEquals(ProjectStatus.SEARCHING_GROUP, byName["Team"])
        assertEquals(ProjectStatus.CREATING_GROUP, byName["Squad"])
        // An old piscine exam stays "in_progress" with its mark; a finished attempt without one failed.
        assertEquals(ProjectStatus.FAILED, byName["C Piscine Exam 02"])
        assertEquals(ProjectStatus.FAILED, byName["Rush 00"])
        assertEquals("libft", profile.projects.first { it.name == "Libft" }.slug)
    }

    @Test
    fun projectsStartWithUngradedOnesThenNewestGrade() {
        assertEquals(
            listOf("Shell", "Rush 00", "Team", "Squad", "Printf", "Libft", "Pipex", "C Piscine Exam 02"),
            profile.projects.map { it.name },
        )
    }

    @Test
    fun onlyTheFourMainCoalitionsCountInTheirOrder() {
        fun coalition(slug: String) = CoalitionDto(name = slug.replaceFirstChar { it.uppercase() }, slug = slug)

        assertEquals("Ignisaria", listOf(coalition("corvus"), coalition("ignisaria")).mainCoalition()?.name)
        assertEquals("Zefiria", listOf(coalition("volans"), coalition("zefiria")).mainCoalition()?.name)
        assertEquals("Zefiria", listOf(coalition("tiamant"), coalition("ignisaria"), coalition("marventis"), coalition("zefiria")).mainCoalition()?.name)
        assertEquals("Marventis", listOf(coalition("tiamant"), coalition("marventis")).mainCoalition()?.name)
        assertNull(listOf(coalition("corvus"), coalition("cassiopeia")).mainCoalition())
        assertNull(emptyList<CoalitionDto>().mainCoalition())
    }

    @Test
    fun aPastBlackHoleDateWithoutEndAtIsStillAStudent() {
        val cadet = IntraJson.decodeFromString<UserDto>(
            """{"id": 3, "login": "cadet", "cursus_users": [{"level": 9.06, "grade": "Cadet",
            "begin_at": "2024-09-16T07:42:00.000Z", "end_at": null, "blackholed_at": "2026-10-03T07:42:00.000Z",
            "cursus": {"id": 21, "name": "42cursus", "slug": "42cursus"}}]}"""
        ).toProfile(now)

        assertEquals(ProfileKind.STUDENT, cadet.kind)
    }

    @Test
    fun sparseAccountFallsBackToDefaults() {
        val staff = IntraJson.decodeFromString<UserDto>("""{"id": 2, "login": "boss", "staff?": true, "wallet": null}""")
            .toProfile(now)

        assertEquals("boss", staff.displayName)
        assertEquals(ProfileKind.STAFF, staff.kind)
        assertEquals(0, staff.wallet)
        assertNull(staff.mainCursus)
        assertTrue(staff.projects.isEmpty())
        assertNull(staff.title)
    }
}
