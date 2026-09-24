package com.ravazque.swiftycompanion.model

import java.time.Instant
import java.time.YearMonth
import kotlin.math.roundToInt

data class Profile(
    val login: String,
    val displayName: String,
    val imageUrl: String?,
    val email: String?,
    val phone: String?,
    val location: String?,
    val wallet: Int,
    val correctionPoints: Int,
    val pool: YearMonth?,
    val campus: String?,
    val title: String?,
    val isStaff: Boolean,
    val cursus: List<Cursus>,
    val projects: List<ProjectRecord>,
    val coalition: Coalition?,
) {
    val mainCursus: Cursus? get() = cursus.firstOrNull()

    fun cursusOrMain(id: Int?): Cursus? = cursus.firstOrNull { it.id == id } ?: mainCursus
}

data class Cursus(
    val id: Int,
    val name: String,
    val slug: String,
    val level: Double,
    val grade: String?,
    val skills: List<Skill>,
) {
    // Levels come with two decimals; working in hundredths avoids 14.19 - 14 = 0.18999...
    private val hundredths: Int get() = (level * 100).roundToInt()
    val levelNumber: Int get() = hundredths / 100
    val levelPercent: Int get() = hundredths % 100

    // The API only lists the skills a user has touched. The main cursus chart always shows all
    // of its axes, alphabetically like the intra, with the untouched ones at zero.
    val chartSkills: List<Skill>
        get() {
            val levels = skills.associate { it.name to it.level }
            val names = if (slug == MAIN_SLUG) MAIN_SKILLS + levels.keys else levels.keys
            return names.toSortedSet().map { Skill(it, levels[it] ?: 0.0) }
        }

    companion object {
        const val MAIN_SLUG = "42cursus"

        val MAIN_SKILLS = setOf(
            "Adaptation & creativity", "Algorithms & AI", "Basics", "Company experience",
            "DB & Data", "Functional programming", "Graphics", "Group & interpersonal",
            "Imperative programming", "Network & system administration",
            "Object-oriented programming", "Organization", "Parallel computing", "Rigor",
            "Ruby", "Security", "Shell", "Technology integration", "Unix", "Web",
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

enum class ProjectStatus { VALIDATED, FAILED, IN_PROGRESS }

data class ProjectRecord(
    val name: String,
    val status: ProjectStatus,
    val finalMark: Int?,
    val markedAt: Instant?,
    val cursusIds: List<Int>,
)

data class Coalition(val name: String, val color: String?, val imageUrl: String?)
