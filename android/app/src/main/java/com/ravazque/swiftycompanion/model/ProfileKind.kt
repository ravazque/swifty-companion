package com.ravazque.swiftycompanion.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class ProfileKind { STUDENT, TRANSCENDER, ALUMNI, BLACKHOLED, FROZEN, PISCINER, STAFF }

// Profiles of one group share the project filter and sort: opening another of the same group keeps them.
enum class ProfileGroup { STUDENTS, GRADUATES, STAFF, BLACKHOLED, FROZEN }

val ProfileKind.group: ProfileGroup
    get() = when (this) {
        ProfileKind.STUDENT, ProfileKind.PISCINER -> ProfileGroup.STUDENTS
        ProfileKind.TRANSCENDER, ProfileKind.ALUMNI -> ProfileGroup.GRADUATES
        ProfileKind.STAFF -> ProfileGroup.STAFF
        ProfileKind.BLACKHOLED -> ProfileGroup.BLACKHOLED
        ProfileKind.FROZEN -> ProfileGroup.FROZEN
    }

// Kinds the search may also show besides students, transcenders and alumni; all off by default.
data class Visibility(val staff: Boolean = false, val blackholed: Boolean = false, val frozen: Boolean = false)

// People who have not started the main cursus are never shown.
fun ProfileKind.isHidden(visibility: Visibility): Boolean = when (this) {
    ProfileKind.STAFF -> !visibility.staff
    ProfileKind.BLACKHOLED -> !visibility.blackholed
    ProfileKind.FROZEN -> !visibility.frozen
    ProfileKind.PISCINER -> true
    ProfileKind.STUDENT, ProfileKind.TRANSCENDER, ProfileKind.ALUMNI -> false
}

// The API has no single field for this. Checked in order: staff flag; alumni flag or grade;
// no main cursus or a kickoff still to come (a pisciner); a main cursus already closed (end_at:
// a past blackholed_at alone does not close it, many active students keep one); an inactive
// account with the cursus still open (on freeze; the API has no freeze field); the Transcender
// grade; anyone else in the main cursus is a student.
fun profileKind(isStaff: Boolean, isAlumni: Boolean, isActive: Boolean, main: Cursus?, now: Instant): ProfileKind {
    if (isStaff) return ProfileKind.STAFF
    if (isAlumni || main?.grade == ALUMNI_GRADE) return ProfileKind.ALUMNI
    val kickoff = main?.beginAt
    if (main == null || kickoff == null || kickoff > now) return ProfileKind.PISCINER
    return when {
        main.endAt?.let { it <= now } == true -> ProfileKind.BLACKHOLED
        !isActive -> ProfileKind.FROZEN
        main.grade == TRANSCENDER_GRADE -> ProfileKind.TRANSCENDER
        else -> ProfileKind.STUDENT
    }
}

private const val ALUMNI_GRADE = "Alumni"
private const val TRANSCENDER_GRADE = "Transcender"

sealed interface AlumniDeadline {
    data object Paused : AlumniDeadline
    data class Due(val date: LocalDate) : AlumniDeadline
}

// A transcender has eight months from the last project that gave experience (the latest passed
// one in the main cursus) to become alumni; an open work experience pauses that count. Null for
// anyone else or without any passed project.
fun Profile.alumniDeadline(zone: ZoneId): AlumniDeadline? {
    val main = cursus.firstOrNull { it.slug == Cursus.MAIN_SLUG }
    if (kind != ProfileKind.TRANSCENDER || main == null) return null
    val projects = projectsOf(main)
    if (projects.any { it.slug in WORK_EXPERIENCE_SLUGS && it.isOpen }) return AlumniDeadline.Paused
    val lastExperience = projects.filter { it.status == ProjectStatus.PASSED }.mapNotNull { it.markedAt }.maxOrNull() ?: return null
    return AlumniDeadline.Due(lastExperience.atZone(zone).toLocalDate().plusMonths(ALUMNI_MONTHS))
}

private val WORK_EXPERIENCE_SLUGS = setOf("work-experience-i", "work-experience-ii")
private const val ALUMNI_MONTHS = 8L
