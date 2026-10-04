package com.ravazque.swiftycompanion.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AlumniDeadlineTest {
    private val main = Cursus(21, "42cursus", Cursus.MAIN_SLUG, 15.0, "Transcender", emptyList())
    private val piscine = Cursus(9, "C Piscine", Cursus.PISCINE_SLUG, 4.0, "Pisciner", emptyList())

    private fun project(slug: String, status: ProjectStatus, date: String?, cursusId: Int = 21) =
        ProjectRecord(slug, status, null, date?.let { Instant.parse("${it}T10:00:00Z") }, listOf(cursusId), slug)

    private fun transcender(vararg projects: ProjectRecord, kind: ProfileKind = ProfileKind.TRANSCENDER) = Profile(
        login = "jdoe", displayName = "John Doe", imageUrl = null, location = null, wallet = 0,
        correctionPoints = 0, pool = null, title = null, kind = kind, cursus = listOf(main, piscine),
        projects = projects.toList(), coalition = null,
    )

    private fun Profile.deadline() = alumniDeadline(ZoneOffset.UTC)

    @Test
    fun eightMonthsFromTheLatestPassedProject() {
        val profile = transcender(
            project("ft_irc", ProjectStatus.PASSED, "2026-03-30"),
            project("ft_linear_regression", ProjectStatus.PASSED, "2026-09-28"),
            project("music-room", ProjectStatus.FAILED, "2026-10-01"),
            project("red-tetris", ProjectStatus.IN_PROGRESS, null),
            project("c-piscine-exam-00", ProjectStatus.PASSED, "2026-12-01", cursusId = 9),
        )
        assertEquals(AlumniDeadline.Due(LocalDate.of(2027, 5, 28)), profile.deadline())
    }

    @Test
    fun anOpenWorkExperiencePausesIt() {
        val passed = project("ft_irc", ProjectStatus.PASSED, "2026-03-30")
        val due = AlumniDeadline.Due(LocalDate.of(2026, 11, 30))
        assertEquals(AlumniDeadline.Paused, transcender(passed, project("work-experience-i", ProjectStatus.IN_PROGRESS, null)).deadline())
        assertEquals(AlumniDeadline.Paused, transcender(passed, project("work-experience-ii", ProjectStatus.WAITING_FOR_CORRECTION, null)).deadline())
        // A finished one, a stale failed one or one of its parts do not pause anything.
        assertEquals(due, transcender(passed, project("work-experience-i", ProjectStatus.FAILED, "2023-04-24")).deadline())
        assertEquals(due, transcender(passed, project("work-experience-i-work-experience-i-duration", ProjectStatus.IN_PROGRESS, null)).deadline())
        assertEquals(
            AlumniDeadline.Due(LocalDate.of(2027, 3, 1)),
            transcender(passed, project("work-experience-i", ProjectStatus.PASSED, "2026-07-01")).deadline(),
        )
    }

    @Test
    fun onlyTranscendersWithAPassedProjectHaveOne() {
        val passed = project("ft_irc", ProjectStatus.PASSED, "2026-03-30")
        assertNull(transcender(passed, kind = ProfileKind.STUDENT).deadline())
        assertNull(transcender(passed, kind = ProfileKind.ALUMNI).deadline())
        assertNull(transcender(project("ft_irc", ProjectStatus.IN_PROGRESS, null)).deadline())
    }
}
