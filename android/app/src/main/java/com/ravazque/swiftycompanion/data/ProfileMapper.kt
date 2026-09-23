package com.ravazque.swiftycompanion.data

import com.ravazque.swiftycompanion.data.net.CoalitionDto
import com.ravazque.swiftycompanion.data.net.CursusUserDto
import com.ravazque.swiftycompanion.data.net.ProjectUserDto
import com.ravazque.swiftycompanion.data.net.UserDto
import com.ravazque.swiftycompanion.model.Coalition
import com.ravazque.swiftycompanion.model.Cursus
import com.ravazque.swiftycompanion.model.Profile
import com.ravazque.swiftycompanion.model.ProjectRecord
import com.ravazque.swiftycompanion.model.ProjectStatus
import com.ravazque.swiftycompanion.model.Skill
import java.time.Instant
import java.time.Month
import java.time.YearMonth

private const val MAIN_CURSUS_SLUG = "42cursus"
private const val HIDDEN = "hidden"

fun UserDto.toProfile(coalition: CoalitionDto?): Profile = Profile(
    login = login,
    displayName = usualFullName?.takeIf { it.isNotBlank() } ?: displayname?.takeIf { it.isNotBlank() } ?: login,
    imageUrl = image?.versions?.medium ?: image?.link,
    email = email?.takeIf { it.isNotBlank() },
    phone = phone?.takeIf { it.isNotBlank() && it != HIDDEN },
    location = location?.takeIf { it.isNotBlank() },
    wallet = wallet,
    correctionPoints = correctionPoint,
    pool = pool(),
    campus = primaryCampus(),
    title = selectedTitle(),
    isStaff = staff,
    cursus = cursusUsers
        .sortedWith(compareByDescending<CursusUserDto> { it.cursus.slug == MAIN_CURSUS_SLUG }.thenByDescending { it.beginAt })
        .map { it.toCursus() },
    projects = projectsUsers.map { it.toRecord() }.sortedByDescending { it.markedAt },
    coalition = coalition?.let { Coalition(it.name, it.color, it.imageUrl) },
)

private fun UserDto.pool(): YearMonth? {
    val year = poolYear?.toIntOrNull() ?: return null
    val month = Month.entries.firstOrNull { it.name.equals(poolMonth, ignoreCase = true) } ?: return null
    return YearMonth.of(year, month)
}

private fun UserDto.primaryCampus(): String? {
    val primaryId = campusUsers.firstOrNull { it.isPrimary }?.campusId
    return (campus.firstOrNull { it.id == primaryId } ?: campus.firstOrNull())?.name
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
)

private fun ProjectUserDto.toRecord() = ProjectRecord(
    name = project.name,
    status = when {
        status != "finished" -> ProjectStatus.IN_PROGRESS
        validated == true -> ProjectStatus.VALIDATED
        else -> ProjectStatus.FAILED
    },
    finalMark = finalMark,
    markedAt = markedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
    cursusIds = cursusIds,
)
