package com.ravazque.swiftycompanion.model

import java.time.Instant

enum class ProfileKind { STUDENT, TRANSCENDER, ALUMNI, BLACKHOLED, PISCINER, STAFF }

// Kinds the search may also show besides students, transcenders and alumni; both off by default.
data class Visibility(val staff: Boolean = false, val blackholed: Boolean = false)

// People who have not started the main cursus are never shown.
fun ProfileKind.isHidden(visibility: Visibility): Boolean = when (this) {
    ProfileKind.STAFF -> !visibility.staff
    ProfileKind.BLACKHOLED -> !visibility.blackholed
    ProfileKind.PISCINER -> true
    ProfileKind.STUDENT, ProfileKind.TRANSCENDER, ProfileKind.ALUMNI -> false
}

// The API has no single field for this. Checked in order: staff flag; alumni flag or grade;
// no main cursus or a kickoff still to come (a pisciner); a main cursus closed or past its black
// hole without graduating; the Transcender grade; anyone else in the main cursus is a student.
fun profileKind(isStaff: Boolean, isAlumni: Boolean, main: Cursus?, now: Instant): ProfileKind {
    if (isStaff) return ProfileKind.STAFF
    if (isAlumni || main?.grade == ALUMNI_GRADE) return ProfileKind.ALUMNI
    val kickoff = main?.beginAt
    if (main == null || kickoff == null || kickoff > now) return ProfileKind.PISCINER
    val closed = main.endAt?.let { it <= now } == true || main.blackholedAt?.let { it <= now } == true
    return when {
        closed -> ProfileKind.BLACKHOLED
        main.grade == TRANSCENDER_GRADE -> ProfileKind.TRANSCENDER
        else -> ProfileKind.STUDENT
    }
}

private const val ALUMNI_GRADE = "Alumni"
private const val TRANSCENDER_GRADE = "Transcender"
