package com.ravazque.swiftycompanion.model

import java.time.Instant

// Blackholed profiles are hidden. Set to true to show them instead, tagged like alumni.
const val SHOW_BLACKHOLED = false

enum class ProfileKind { STUDENT, TRANSCENDER, ALUMNI, BLACKHOLED, PISCINER, STAFF }

// The app only shows active students: staff and people who have not started the main cursus
// are never shown, blackholed ones depend on SHOW_BLACKHOLED.
fun ProfileKind.isHidden(showBlackholed: Boolean = SHOW_BLACKHOLED): Boolean = when (this) {
    ProfileKind.STAFF, ProfileKind.PISCINER -> true
    ProfileKind.BLACKHOLED -> !showBlackholed
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
