package com.ravazque.swiftycompanion.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ProjectListTest {
    private val main = Cursus(21, "42cursus", "42cursus", 5.0, null, emptyList())
    private val piscine = Cursus(9, "C Piscine", "c-piscine", 4.0, null, emptyList())

    private fun project(name: String, status: ProjectStatus, mark: Int?, date: String?, vararg cursusIds: Int) =
        ProjectRecord(name, status, mark, date?.let { Instant.parse("${it}T10:00:00Z") }, cursusIds.toList())

    private val profile = Profile(
        login = "jdoe", displayName = "John Doe", imageUrl = null, location = null, wallet = 0,
        correctionPoints = 0, pool = null, title = null, kind = ProfileKind.STUDENT,
        cursus = listOf(main, piscine), coalition = null,
        projects = listOf(
            project("libft", ProjectStatus.PASSED, 125, "2025-01-10", 21),
            project("Shell 00", ProjectStatus.FAILED, 0, "2024-07-05", 9),
            project("Printf", ProjectStatus.FAILED, 42, "2025-02-01", 21),
            project("Shared", ProjectStatus.IN_PROGRESS, null, null, 9, 21),
            project("Orphan", ProjectStatus.PASSED, 100, "2023-01-01", 99),
            project("Loose", ProjectStatus.WAITING_FOR_CORRECTION, null, null),
        ),
    )

    private fun List<ProjectRecord>.names() = map { it.name }

    @Test
    fun eachCursusShowsOnlyItsOwnProjects() {
        assertEquals(listOf("Shell 00", "Shared"), profile.projectsOf(piscine).names())
    }

    @Test
    fun theMainCursusAlsoGetsProjectsOfNoOtherCursus() {
        assertEquals(listOf("libft", "Printf", "Shared", "Orphan", "Loose"), profile.projectsOf(main).names())
        assertEquals(profile.projects, profile.projectsOf(null))
    }

    @Test
    fun statusFilter() {
        val projects = profile.projectsOf(main)
        assertEquals(listOf("Printf"), projects.withStatus(ProjectStatus.FAILED).names())
        assertEquals(listOf("Loose"), projects.withStatus(ProjectStatus.WAITING_FOR_CORRECTION).names())
        assertEquals(projects, projects.withStatus(null))
    }

    @Test
    fun sortOrders() {
        val projects = profile.projectsOf(main)
        assertEquals(listOf("Shared", "Loose", "Printf", "libft", "Orphan"), projects.orderedBy(ProjectSort.DATE).names())
        assertEquals(listOf("libft", "Orphan", "Printf", "Shared", "Loose"), projects.orderedBy(ProjectSort.GRADE).names())
        assertEquals(listOf("libft", "Loose", "Orphan", "Printf", "Shared"), projects.orderedBy(ProjectSort.NAME).names())
    }
}
