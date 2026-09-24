package com.ravazque.swiftycompanion.ui.profile

import com.ravazque.swiftycompanion.model.Coalition
import com.ravazque.swiftycompanion.model.Cursus
import com.ravazque.swiftycompanion.model.Profile
import com.ravazque.swiftycompanion.model.Skill
import java.time.YearMonth

// Fake data for Android Studio previews only.
internal val previewProfile = Profile(
    login = "jdoe",
    displayName = "John Doe",
    imageUrl = null,
    email = "jdoe@student.42.fr",
    phone = null,
    location = "c1r2s3",
    wallet = 120,
    correctionPoints = 5,
    pool = YearMonth.of(2024, 7),
    campus = "Madrid",
    title = "Mastermind jdoe",
    isStaff = false,
    cursus = listOf(
        Cursus(21, "42cursus", "42cursus", 7.42, "Member", listOf(Skill("Unix", 6.3), Skill("Rigor", 4.1))),
        Cursus(9, "C Piscine", "c-piscine", 4.67, "Pisciner", listOf(Skill("Unix", 5.0))),
    ),
    projects = emptyList(),
    coalition = Coalition("Blue", "#3F8EFC", null),
)
