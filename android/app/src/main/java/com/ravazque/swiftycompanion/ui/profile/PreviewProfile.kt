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
        Cursus(
            21, "42cursus", "42cursus", 7.42, "Member",
            listOf(
                Skill("Unix", 9.1), Skill("Imperative programming", 8.4), Skill("Rigor", 7.2),
                Skill("Algorithms & AI", 6.3), Skill("Network & system administration", 5.5),
                Skill("Group & interpersonal", 5.1), Skill("Adaptation & creativity", 3.9),
                Skill("Organization", 3.2), Skill("Graphics", 2.4),
            ),
        ),
        Cursus(9, "C Piscine", "c-piscine", 4.67, "Pisciner", listOf(Skill("Unix", 5.0))),
    ),
    projects = emptyList(),
    coalition = Coalition("Blue", "#3F8EFC", null),
)
