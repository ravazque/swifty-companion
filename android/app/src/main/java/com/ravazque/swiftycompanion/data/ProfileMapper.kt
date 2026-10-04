package com.ravazque.swiftycompanion.data

import com.ravazque.swiftycompanion.data.net.CoalitionDto
import com.ravazque.swiftycompanion.data.net.CursusUserDto
import com.ravazque.swiftycompanion.data.net.ProjectUserDto
import com.ravazque.swiftycompanion.data.net.UserDto
import com.ravazque.swiftycompanion.model.Coalition
import com.ravazque.swiftycompanion.model.Cursus
import com.ravazque.swiftycompanion.model.Profile
import com.ravazque.swiftycompanion.model.ProjectRecord
import com.ravazque.swiftycompanion.model.ProjectSort
import com.ravazque.swiftycompanion.model.ProjectStatus
import com.ravazque.swiftycompanion.model.Skill
import com.ravazque.swiftycompanion.model.orderedBy
import com.ravazque.swiftycompanion.model.profileKind
import java.time.Instant
import java.time.Month
import java.time.YearMonth

fun UserDto.toProfile(now: Instant): Profile {
    val cursus = cursusUsers
        .sortedWith(compareByDescending<CursusUserDto> { it.cursus.slug == Cursus.MAIN_SLUG }.thenByDescending { it.beginAt })
        .map { it.toCursus() }
    return Profile(
        login = login,
        displayName = usualFullName?.takeIf { it.isNotBlank() } ?: displayname?.takeIf { it.isNotBlank() } ?: login,
        imageUrl = image?.versions?.medium ?: image?.link,
        location = location?.takeIf { it.isNotBlank() },
        wallet = wallet,
        correctionPoints = correctionPoint,
        pool = pool(),
        title = selectedTitle(),
        kind = profileKind(staff, alumni, cursus.firstOrNull { it.slug == Cursus.MAIN_SLUG }, now),
        cursus = cursus,
        projects = projectsUsers.map { it.toRecord() }.orderedBy(ProjectSort.DATE),
        coalition = null,
    )
}

// Only the four coalitions of the main cursus count, in this order if a user is in more than one;
// any other (the piscine ones, for example) is ignored and the profile shows no coalition.
private val MAIN_COALITIONS = listOf("zefiria", "marventis", "ignisaria", "tiamant")

fun List<CoalitionDto>.mainCoalition(): Coalition? =
    MAIN_COALITIONS.firstNotNullOfOrNull { slug -> firstOrNull { it.slug == slug } }
        ?.let { Coalition(it.name, it.color, it.imageUrl) }

private fun UserDto.pool(): YearMonth? {
    val year = poolYear?.toIntOrNull() ?: return null
    val month = Month.entries.firstOrNull { it.name.equals(poolMonth, ignoreCase = true) } ?: return null
    return YearMonth.of(year, month)
}

private fun UserDto.selectedTitle(): String? {
    val selectedId = titlesUsers.firstOrNull { it.selected }?.titleId ?: return null
    return titles.firstOrNull { it.id == selectedId }?.name?.replace("%login", login)
}

private fun CursusUserDto.toCursus() = Cursus(
    id = cursus.id,
    name = cursus.name,
    slug = cursus.slug,
    level = level,
    grade = grade,
    skills = skills.map { Skill(it.name, it.level) }.sortedByDescending { it.level },
    beginAt = instant(beginAt),
    endAt = instant(endAt),
    blackholedAt = instant(blackholedAt),
)

private fun ProjectUserDto.toRecord() = ProjectRecord(
    name = project.name,
    // An open attempt keeps its own status. Otherwise the latest grade decides: old piscine
    // exams stay "in_progress" with their mark.
    status = when {
        status == "waiting_for_correction" -> ProjectStatus.WAITING_FOR_CORRECTION
        status == "searching_a_group" -> ProjectStatus.SEARCHING_GROUP
        status == "creating_group" -> ProjectStatus.CREATING_GROUP
        validated == true -> ProjectStatus.PASSED
        validated == false || status == "finished" -> ProjectStatus.FAILED
        else -> ProjectStatus.IN_PROGRESS
    },
    finalMark = finalMark,
    markedAt = instant(markedAt),
    cursusIds = cursusIds,
    slug = project.slug,
)

private fun instant(text: String?): Instant? = text?.let { runCatching { Instant.parse(it) }.getOrNull() }
