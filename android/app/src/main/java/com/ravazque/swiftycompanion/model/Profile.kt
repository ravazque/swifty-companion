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
}

data class Skill(val name: String, val level: Double) {
    val percent: Double get() = (level / MAX_LEVEL * 100).coerceIn(0.0, 100.0)

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
