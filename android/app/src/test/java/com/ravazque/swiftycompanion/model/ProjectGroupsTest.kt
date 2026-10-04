package com.ravazque.swiftycompanion.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectGroupsTest {
    private val main = Cursus(21, "42cursus", "42cursus", 5.0, null, emptyList())
    private val piscine = Cursus(9, "C Piscine", "c-piscine", 4.0, null, emptyList())

    private fun project(name: String, status: ProjectStatus, vararg cursusIds: Int) =
        ProjectRecord(name, status, null, null, cursusIds.toList())

    private val profile = Profile(
        login = "jdoe", displayName = "John Doe", imageUrl = null, email = null, phone = null,
        location = null, wallet = 0, correctionPoints = 0, pool = null, campus = null, title = null,
        isStaff = false, cursus = listOf(main, piscine), coalition = null,
        projects = listOf(
            project("Libft", ProjectStatus.VALIDATED, 21),
            project("Shell 00", ProjectStatus.FAILED, 9),
            project("Printf", ProjectStatus.FAILED, 21),
            project("Shared", ProjectStatus.IN_PROGRESS, 9, 21),
            project("Orphan", ProjectStatus.VALIDATED, 99),
            project("Loose", ProjectStatus.VALIDATED),
        ),
    )

    private fun List<ProjectGroup>.shape() = map { group -> group.cursus?.slug to group.projects.map { it.name } }

    @Test
    fun selectedCursusComesFirstAndUnknownCursusLast() {
        assertEquals(
            listOf(
                "c-piscine" to listOf("Shell 00", "Shared"),
                "42cursus" to listOf("Libft", "Printf"),
                null to listOf("Orphan", "Loose"),
            ),
            profile.projectGroups(firstCursusId = 9, status = null).shape(),
        )
    }

    @Test
    fun everyProjectAppearsExactlyOnce() {
        val shown = profile.projectGroups(firstCursusId = 21, status = null).flatMap { it.projects }
        assertEquals(profile.projects.size, shown.size)
        assertEquals(profile.projects.toSet(), shown.toSet())
        assertEquals("42cursus" to listOf("Libft", "Printf", "Shared"), profile.projectGroups(21, null).shape().first())
    }

    @Test
    fun statusFilterDropsEmptyGroups() {
        assertEquals(
            listOf("42cursus" to listOf("Printf"), "c-piscine" to listOf("Shell 00")),
            profile.projectGroups(firstCursusId = null, status = ProjectStatus.FAILED).shape(),
        )
        assertTrue(profile.copy(projects = emptyList()).projectGroups(null, null).isEmpty())
    }
}
