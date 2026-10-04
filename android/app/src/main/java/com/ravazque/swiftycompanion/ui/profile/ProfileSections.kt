package com.ravazque.swiftycompanion.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.ravazque.swiftycompanion.R
import com.ravazque.swiftycompanion.model.AlumniDeadline
import com.ravazque.swiftycompanion.model.Cursus
import com.ravazque.swiftycompanion.model.Profile
import com.ravazque.swiftycompanion.model.alumniDeadline
import com.ravazque.swiftycompanion.ui.theme.LineSoft
import com.ravazque.swiftycompanion.ui.theme.Surface1
import com.ravazque.swiftycompanion.ui.theme.Surface2
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun CursusSelector(
    cursus: List<Cursus>,
    selectedId: Int?,
    accent: Color,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        cursus.forEach { item ->
            FilterChip(
                selected = item.id == selectedId,
                onClick = { onSelect(item.id) },
                label = { Text(item.name) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent.copy(alpha = 0.18f),
                    selectedLabelColor = accent,
                ),
            )
        }
    }
}

@Composable
fun LevelBlock(cursus: Cursus, accent: Color, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val word = stringResource(if (cursus.isPiscine) R.string.level_word_piscine else R.string.level_word_cursus)
    val label = stringResource(if (cursus.isPiscine) R.string.level_in_piscine else R.string.level_in_cursus, word)
    Section(modifier) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = String.format(locale, "%.2f", cursus.level),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        // A soft glow in the accent color lifts the number off the panel.
                        shadow = Shadow(accent.copy(alpha = 0.45f), Offset(0f, 4f), blurRadius = 18f),
                    ),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-0.04).em,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = buildAnnotatedString {
                        append(label)
                        val start = label.indexOf(word)
                        if (start >= 0) addStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold), start, start + word.length)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            LinearProgressIndicator(
                progress = { cursus.levelPercent / 100f },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(8.dp),
                color = accent,
                trackColor = Surface2,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
    }
}

// The pool always; outside the piscine also the kickoff, the alumni deadline of a transcender and
// the black hole.
@Composable
fun DetailsSection(profile: Profile, cursus: Cursus?, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val dates = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    Section(modifier) {
        Column(Modifier.padding(vertical = 4.dp)) {
            DetailRow(stringResource(R.string.profile_pool), profile.pool?.format(locale) ?: stringResource(R.string.profile_unavailable))
            if (cursus != null && !cursus.isPiscine) {
                cursus.beginAt?.let { DetailRow(stringResource(R.string.profile_kickoff), it.localDate().format(dates)) }
                if (cursus.slug == Cursus.MAIN_SLUG) {
                    when (val deadline = profile.alumniDeadline(ZoneId.systemDefault())) {
                        AlumniDeadline.Paused -> DetailRow(stringResource(R.string.profile_alumni), stringResource(R.string.profile_paused))
                        is AlumniDeadline.Due -> DetailRow(stringResource(R.string.profile_alumni), daysLeft(deadline.date, dates))
                        null -> Unit
                    }
                }
                cursus.blackholedAt?.let { DetailRow(stringResource(R.string.profile_blackhole), daysLeft(it.localDate(), dates)) }
            }
        }
    }
}

@Composable
private fun daysLeft(date: LocalDate, dates: DateTimeFormatter): String {
    val days = ChronoUnit.DAYS.between(LocalDate.now(), date).toInt()
    if (days < 0) return date.format(dates)
    return pluralStringResource(R.plurals.profile_days_left, days, date.format(dates), days)
}

private fun Instant.localDate(): LocalDate = atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
internal fun SectionHeader(title: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun Section(modifier: Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Surface1,
        border = BorderStroke(1.dp, LineSoft),
        content = content,
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}

private fun YearMonth.format(locale: Locale): String =
    DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(this).replaceFirstChar { it.titlecase(locale) }
