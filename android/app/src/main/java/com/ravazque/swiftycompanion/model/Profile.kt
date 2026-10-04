package com.ravazque.swiftycompanion.model

import java.time.Instant
import java.time.YearMonth
import kotlin.math.roundToInt

data class Profile(
    val login: String,
    val displayName: String,
    val imageUrl: String?,
    val location: String?,
    val wallet: Int,
    val correctionPoints: Int,
    val pool: YearMonth?,
    val title: String?,
    val kind: ProfileKind,
    val cursus: List<Cursus>,
    val projects: List<ProjectRecord>,
    val coalition: Coalition?,
) {
    val mainCursus: Cursus? get() = cursus.firstOrNull()

    fun cursusOrMain(id: Int?): Cursus? = cursus.firstOrNull { it.id == id } ?: mainCursus

    // The projects of one cursus. Projects of none of the user's cursus are listed with the main
    // one, so every project can be reached from some cursus.
    fun projectsOf(selected: Cursus?): List<ProjectRecord> {
        if (selected == null) return projects
        val ids = cursus.map { it.id }.toSet()
        return projects.filter { project ->
            selected.id in project.cursusIds || (selected == mainCursus && project.cursusIds.none { it in ids })
        }
    }
}

data class Cursus(
    val id: Int,
    val name: String,
    val slug: String,
    val level: Double,
    val grade: String?,
    val skills: List<Skill>,
    val beginAt: Instant? = null,
    val endAt: Instant? = null,
    val blackholedAt: Instant? = null,
) {
    // Levels come with two decimals; working in hundredths avoids 14.19 - 14 = 0.18999...
    private val hundredths: Int get() = (level * 100).roundToInt()
    val levelNumber: Int get() = hundredths / 100
    val levelPercent: Int get() = hundredths % 100

    val isPiscine: Boolean get() = slug == PISCINE_SLUG

    // The API only lists the skills a user has touched. The main cursus and the piscine charts
    // always show all of their axes, with the untouched ones at zero: the main cursus
    // alphabetically, the piscine in the intra's order.
    val chartSkills: List<Skill>
        get() {
            val levels = skills.associate { it.name to it.level }
            val names = when (slug) {
                MAIN_SLUG -> (MAIN_SKILLS + levels.keys).sorted()
                PISCINE_SLUG -> PISCINE_SKILLS + (levels.keys - PISCINE_SKILLS.toSet()).sorted()
                else -> levels.keys.sorted()
            }
            return names.map { Skill(it, levels[it] ?: 0.0) }
        }

    // The piscine chart is a hexagon with flat top and bottom: its first axis sits half a step
    // before the top, at the upper left.
    val chartStartsHalfStepBefore: Boolean get() = slug == PISCINE_SLUG

    companion object {
        const val MAIN_SLUG = "42cursus"
        const val PISCINE_SLUG = "c-piscine"

        val MAIN_SKILLS = setOf(
            "Adaptation & creativity", "Algorithms & AI", "Basics", "Company experience",
            "DB & Data", "Functional programming", "Graphics", "Group & interpersonal",
            "Imperative programming", "Network & system administration",
            "Object-oriented programming", "Organization", "Parallel computing", "Rigor",
            "Ruby", "Security", "Shell", "Technology integration", "Unix", "Web",
        )

        val PISCINE_SKILLS = listOf(
            "Unix", "Adaptation & creativity", "Algorithms & AI", "Group & interpersonal",
            "Imperative programming", "Rigor",
        )
    }
}

data class Skill(val name: String, val level: Double) {
    val ratio: Double get() = (level / MAX_LEVEL).coerceIn(0.0, 1.0)
    val percent: Double get() = ratio * 100

    companion object {
        const val MAX_LEVEL = 21.0
    }
}

enum class ProjectStatus { PASSED, FAILED, IN_PROGRESS, WAITING_FOR_CORRECTION, SEARCHING_GROUP, CREATING_GROUP }

enum class ProjectSort { DATE, GRADE, NAME }

data class ProjectRecord(
    val name: String,
    val status: ProjectStatus,
    val finalMark: Int?,
    val markedAt: Instant?,
    val cursusIds: List<Int>,
    val slug: String = "",
) {
    val isOpen: Boolean get() = status != ProjectStatus.PASSED && status != ProjectStatus.FAILED
}

fun List<ProjectRecord>.withStatus(status: ProjectStatus?): List<ProjectRecord> =
    if (status == null) this else filter { it.status == status }

// Date: ungraded attempts first, then the newest grade. Grade: highest mark first. Name: A to Z.
fun List<ProjectRecord>.orderedBy(sort: ProjectSort): List<ProjectRecord> = when (sort) {
    ProjectSort.DATE -> sortedWith(compareBy<ProjectRecord> { it.markedAt != null }.thenByDescending { it.markedAt })
    ProjectSort.GRADE -> sortedWith(compareBy<ProjectRecord> { it.finalMark == null }.thenByDescending { it.finalMark })
    ProjectSort.NAME -> sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
}

data class Coalition(val name: String, val color: String?, val imageUrl: String?)
