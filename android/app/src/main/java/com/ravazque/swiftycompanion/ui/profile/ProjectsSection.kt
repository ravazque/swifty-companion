package com.ravazque.swiftycompanion.ui.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ravazque.swiftycompanion.R
import com.ravazque.swiftycompanion.model.Profile
import com.ravazque.swiftycompanion.model.ProjectGroup
import com.ravazque.swiftycompanion.model.ProjectRecord
import com.ravazque.swiftycompanion.model.ProjectStatus
import com.ravazque.swiftycompanion.ui.theme.Bad
import com.ravazque.swiftycompanion.ui.theme.DefaultAccent
import com.ravazque.swiftycompanion.ui.theme.Ok
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Projects tab: status filter, then one section per cursus (the selected one first). Each group
// is its own list item, so long project lists stay lazy.
fun LazyListScope.projectItems(
    profile: Profile,
    firstCursusId: Int?,
    filter: ProjectStatus?,
    accent: Color,
    onSelectFilter: (ProjectStatus?) -> Unit,
    modifier: Modifier,
) {
    if (profile.projects.isEmpty()) {
        item(key = "projects-empty") { EmptySection(R.string.projects_empty, modifier) }
        return
    }
    item(key = "project-filters") { ProjectFilters(profile.projects, filter, accent, onSelectFilter, modifier) }
    val groups = profile.projectGroups(firstCursusId, filter)
    if (groups.isEmpty()) {
        item(key = "projects-none") { EmptySection(R.string.projects_empty_filter, modifier) }
    }
    groups.forEach { group ->
        item(key = "projects-${group.cursus?.id ?: "other"}") { ProjectGroupSection(group, modifier) }
    }
}

@Composable
private fun ProjectFilters(
    projects: List<ProjectRecord>,
    selected: ProjectStatus?,
    accent: Color,
    onSelect: (ProjectStatus?) -> Unit,
    modifier: Modifier,
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (listOf(null) + ProjectStatus.entries).forEach { status ->
            FilterChip(
                selected = status == selected,
                onClick = { onSelect(status) },
                label = {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(status.filterLabel))
                        Text(projects.count { status == null || it.status == status }.toString(), fontFamily = FontFamily.Monospace)
                    }
                },
                colors = accentChipColors(accent),
            )
        }
    }
}

@Composable
private fun ProjectGroupSection(group: ProjectGroup, modifier: Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val dates = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    Section(modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionHeader(group.cursus?.name ?: stringResource(R.string.projects_other), group.projects.size)
            group.projects.forEach { ProjectRow(it, dates) }
        }
    }
}

@Composable
private fun ProjectRow(project: ProjectRecord, dates: DateTimeFormatter) {
    val color = project.status.color
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(project.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            project.finalMark?.let {
                Text(it.toString(), style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, color = color)
            }
        }
        Row {
            Text(stringResource(project.status.label), style = MaterialTheme.typography.bodySmall, color = color)
            project.markedAt?.let {
                Text(
                    text = " · " + dates.format(it.atZone(ZoneId.systemDefault())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptySection(@StringRes message: Int, modifier: Modifier) {
    Section(modifier) {
        Text(
            text = stringResource(message),
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val ProjectStatus?.filterLabel: Int
    get() = when (this) {
        null -> R.string.filter_all
        ProjectStatus.VALIDATED -> R.string.filter_validated
        ProjectStatus.FAILED -> R.string.filter_failed
        ProjectStatus.IN_PROGRESS -> R.string.filter_in_progress
    }

private val ProjectStatus.label: Int
    get() = when (this) {
        ProjectStatus.VALIDATED -> R.string.status_validated
        ProjectStatus.FAILED -> R.string.status_failed
        ProjectStatus.IN_PROGRESS -> R.string.status_in_progress
    }

// Fixed colors: the coalition accent could itself be green or red.
private val ProjectStatus.color: Color
    get() = when (this) {
        ProjectStatus.VALIDATED -> Ok
        ProjectStatus.FAILED -> Bad
        ProjectStatus.IN_PROGRESS -> DefaultAccent
    }
