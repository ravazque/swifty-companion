package com.ravazque.swiftycompanion.ui.profile

import com.ravazque.swiftycompanion.model.Coalition
import com.ravazque.swiftycompanion.model.Cursus
import com.ravazque.swiftycompanion.model.Profile
import com.ravazque.swiftycompanion.model.ProfileKind
import com.ravazque.swiftycompanion.model.ProjectRecord
import com.ravazque.swiftycompanion.model.ProjectStatus
import com.ravazque.swiftycompanion.model.Skill
import java.time.Instant
import java.time.YearMonth

// Fake data for Android Studio previews only.
internal val previewProfile = Profile(
    login = "jdoe",
    displayName = "John Doe",
    imageUrl = null,
    location = "c1r2s3",
    wallet = 120,
    correctionPoints = 5,
    pool = YearMonth.of(2024, 7),
    title = "Mastermind jdoe",
    kind = ProfileKind.TRANSCENDER,
    cursus = listOf(
        Cursus(
            21, "42cursus", "42cursus", 7.42, "Cadet",
            listOf(
                Skill("Unix", 9.1), Skill("Imperative programming", 8.4), Skill("Rigor", 7.2),
                Skill("Algorithms & AI", 6.3), Skill("Network & system administration", 5.5),
                Skill("Group & interpersonal", 5.1), Skill("Adaptation & creativity", 3.9),
                Skill("Organization", 3.2), Skill("Graphics", 2.4),
            ),
            beginAt = Instant.parse("2024-09-16T07:42:00Z"),
            blackholedAt = Instant.parse("2027-03-01T07:42:00Z"),
        ),
        Cursus(
            9, "C Piscine", "c-piscine", 4.67, "Pisciner",
            listOf(Skill("Unix", 5.0), Skill("Rigor", 3.2), Skill("Algorithms & AI", 2.1)),
            beginAt = Instant.parse("2024-07-01T08:00:00Z"),
        ),
    ),
    projects = listOf(
        ProjectRecord("minishell", ProjectStatus.IN_PROGRESS, null, null, listOf(21)),
        ProjectRecord("Born2beroot", ProjectStatus.WAITING_FOR_CORRECTION, null, null, listOf(21)),
        ProjectRecord("Libft", ProjectStatus.PASSED, 125, Instant.parse("2026-09-10T10:00:00Z"), listOf(21)),
        ProjectRecord("ft_printf", ProjectStatus.FAILED, 0, Instant.parse("2024-12-02T10:00:00Z"), listOf(21)),
        ProjectRecord("C Piscine C 00", ProjectStatus.PASSED, 100, Instant.parse("2024-07-05T10:00:00Z"), listOf(9)),
    ),
    coalition = Coalition("Zefiria", "#E39F0B", null),
)
