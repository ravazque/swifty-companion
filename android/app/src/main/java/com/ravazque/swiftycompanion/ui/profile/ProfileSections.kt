package com.ravazque.swiftycompanion.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.ravazque.swiftycompanion.R
import com.ravazque.swiftycompanion.model.Cursus
import com.ravazque.swiftycompanion.model.Profile
import com.ravazque.swiftycompanion.model.Skill
import com.ravazque.swiftycompanion.ui.theme.LineSoft
import com.ravazque.swiftycompanion.ui.theme.Surface1
import com.ravazque.swiftycompanion.ui.theme.Surface2
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CursusSelector(
    cursus: List<Cursus>,
    selectedId: Int?,
    accent: Color,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
    Section(modifier) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = String.format(locale, "%.2f", cursus.level),
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-0.04).em,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = stringResource(R.string.level_block_label, cursus.name),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            LinearProgressIndicator(
                progress = { cursus.levelPercent / 100f },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(8.dp),
                color = accent,
                trackColor = Surface2,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Text(
                text = stringResource(R.string.level_block_progress, cursus.levelPercent, cursus.levelNumber + 1),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DetailsSection(profile: Profile, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val unavailable = stringResource(R.string.profile_unavailable)
    Section(modifier) {
        Column(Modifier.padding(vertical = 4.dp)) {
            DetailRow(stringResource(R.string.profile_email), profile.email ?: unavailable)
            DetailRow(stringResource(R.string.profile_phone), profile.phone ?: stringResource(R.string.profile_hidden))
            DetailRow(stringResource(R.string.profile_campus), profile.campus ?: unavailable)
            DetailRow(stringResource(R.string.profile_pool), profile.pool?.format(locale) ?: unavailable)
        }
    }
}

@Composable
fun SkillsSection(cursus: Cursus?, accent: Color, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val skills = cursus?.skills.orEmpty()
    Section(modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.skills_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                cursus?.let {
                    Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (skills.isEmpty()) {
                Text(stringResource(R.string.skills_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            skills.forEach { SkillRow(it, accent, locale) }
        }
    }
}

@Composable
private fun SkillRow(skill: Skill, accent: Color, locale: Locale) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(skill.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                text = String.format(locale, "%.2f", skill.level),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { skill.ratio.toFloat() },
                modifier = Modifier.weight(1f).height(4.dp),
                color = accent,
                trackColor = Surface2,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Text(
                text = String.format(locale, "%.2f%%", skill.percent),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 64.dp),
            )
        }
    }
}

@Composable
private fun Section(modifier: Modifier, content: @Composable () -> Unit) {
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
