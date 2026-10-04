package com.ravazque.swiftycompanion.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ProfileKindTest {
    private val now = Instant.parse("2026-10-04T00:00:00Z")
    private val past = Instant.parse("2026-01-01T00:00:00Z")
    private val future = Instant.parse("2027-01-01T00:00:00Z")

    private fun main(
        grade: String? = "Cadet",
        beginAt: Instant? = past,
        endAt: Instant? = null,
        blackholedAt: Instant? = future,
    ) = Cursus(21, "42cursus", Cursus.MAIN_SLUG, 3.0, grade, emptyList(), beginAt, endAt, blackholedAt)

    private fun kind(main: Cursus?, staff: Boolean = false, alumni: Boolean = false) = profileKind(staff, alumni, main, now)

    @Test
    fun activeStudentsAndTranscenders() {
        assertEquals(ProfileKind.STUDENT, kind(main()))
        assertEquals(ProfileKind.STUDENT, kind(main(blackholedAt = null)))
        assertEquals(ProfileKind.TRANSCENDER, kind(main(grade = "Transcender")))
    }

    @Test
    fun noKickoffYetIsAPisciner() {
        assertEquals(ProfileKind.PISCINER, kind(null))
        assertEquals(ProfileKind.PISCINER, kind(main(beginAt = null)))
        assertEquals(ProfileKind.PISCINER, kind(main(beginAt = future)))
        assertEquals(ProfileKind.STUDENT, kind(main(beginAt = now)))
    }

    @Test
    fun aClosedCursusOrAPastBlackHoleIsBlackholed() {
        assertEquals(ProfileKind.BLACKHOLED, kind(main(endAt = past)))
        assertEquals(ProfileKind.BLACKHOLED, kind(main(blackholedAt = past)))
        assertEquals(ProfileKind.BLACKHOLED, kind(main(grade = "Transcender", blackholedAt = past)))
    }

    @Test
    fun staffAndAlumniWinOverEverythingElse() {
        assertEquals(ProfileKind.STAFF, kind(main(), staff = true))
        assertEquals(ProfileKind.STAFF, kind(null, staff = true))
        assertEquals(ProfileKind.ALUMNI, kind(main(endAt = past), alumni = true))
        assertEquals(ProfileKind.ALUMNI, kind(main(grade = "Alumni", endAt = past)))
    }

    @Test
    fun hiddenKindsFollowTheSearchOptions() {
        fun shown(visibility: Visibility) = ProfileKind.entries.filterNot { it.isHidden(visibility) }.toSet()
        val always = setOf(ProfileKind.STUDENT, ProfileKind.TRANSCENDER, ProfileKind.ALUMNI)

        assertEquals(always, shown(Visibility()))
        assertEquals(always + ProfileKind.STAFF, shown(Visibility(staff = true)))
        assertEquals(always + ProfileKind.BLACKHOLED, shown(Visibility(blackholed = true)))
        assertEquals(always + ProfileKind.STAFF + ProfileKind.BLACKHOLED, shown(Visibility(staff = true, blackholed = true)))
    }
}
