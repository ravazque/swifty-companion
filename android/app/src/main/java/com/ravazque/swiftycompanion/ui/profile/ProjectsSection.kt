package com.ravazque.swiftycompanion.ui.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ravazque.swiftycompanion.R
import com.ravazque.swiftycompanion.model.ProjectRecord
import com.ravazque.swiftycompanion.model.ProjectSort
import com.ravazque.swiftycompanion.model.ProjectStatus
import com.ravazque.swiftycompanion.model.orderedBy
import com.ravazque.swiftycompanion.model.withStatus
import com.ravazque.swiftycompanion.ui.theme.Amber
import com.ravazque.swiftycompanion.ui.theme.Bad
import com.ravazque.swiftycompanion.ui.theme.Ok
import com.ravazque.swiftycompanion.ui.theme.Violet
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Projects of the selected cursus: a status filter and a sort order, then the list. The status
// menu counts every project of the cursus, whatever the current filter.
fun LazyListScope.projectItems(
    projects: List<ProjectRecord>,
    filter: ProjectStatus?,
    sort: ProjectSort,
    accent: Color,
    onSelectFilter: (ProjectStatus?) -> Unit,
    onSelectSort: (ProjectSort) -> Unit,
    modifier: Modifier,
) {
    if (projects.isEmpty()) {
        item(key = "projects-empty") { EmptySection(R.string.projects_empty, modifier) }
        return
    }
    item(key = "project-controls") { ProjectControls(projects, filter, sort, accent, onSelectFilter, onSelectSort, modifier) }
    val shown = projects.withStatus(filter).orderedBy(sort)
    item(key = "projects") {
        if (shown.isEmpty()) EmptySection(R.string.projects_empty_filter, modifier) else ProjectList(shown, modifier)
    }
}

@Composable
private fun ProjectControls(
    projects: List<ProjectRecord>,
    filter: ProjectStatus?,
    sort: ProjectSort,
    accent: Color,
    onSelectFilter: (ProjectStatus?) -> Unit,
    onSelectSort: (ProjectSort) -> Unit,
    modifier: Modifier,
) {
    val label: @Composable (ProjectStatus?) -> String = { status ->
        "${stringResource(status.filterLabel)} (${projects.count { status == null || it.status == status }})"
    }
    val status: @Composable (Modifier) -> Unit = {
        Dropdown(stringResource(R.string.projects_status), filter, listOf(null) + ProjectStatus.entries, label, accent, onSelectFilter, it)
    }
    val order: @Composable (Modifier) -> Unit = {
        Dropdown(stringResource(R.string.projects_sort), sort, ProjectSort.entries, { s -> stringResource(s.label) }, accent, onSelectSort, it)
    }
    // Side by side only where the longest status ("Waiting for correction (12)") still fits.
    BoxWithConstraints(modifier) {
        if (maxWidth >= 520.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                status(Modifier.weight(1.3f))
                order(Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                status(Modifier.fillMaxWidth())
                order(Modifier.fillMaxWidth())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Dropdown(
    label: String,
    selected: T,
    options: List<T>,
    text: @Composable (T) -> String,
    accent: Color,
    onSelect: (T) -> Unit,
    modifier: Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val close = {
        expanded = false
        focus.clearFocus()
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = text(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent, focusedLabelColor = accent),
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = close) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text(option), color = if (option == selected) accent else Color.Unspecified) },
                    onClick = {
                        onSelect(option)
                        close()
                    },
                )
            }
        }
    }
}

@Composable
private fun ProjectList(projects: List<ProjectRecord>, modifier: Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val dates = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    Section(modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionHeader(stringResource(R.string.projects_title), projects.size)
            projects.forEach { ProjectRow(it, dates) }
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
        ProjectStatus.PASSED -> R.string.filter_passed
        ProjectStatus.FAILED -> R.string.filter_failed
        ProjectStatus.IN_PROGRESS -> R.string.filter_in_progress
        ProjectStatus.WAITING_FOR_CORRECTION -> R.string.filter_waiting_for_correction
        ProjectStatus.SEARCHING_GROUP -> R.string.filter_searching_group
        ProjectStatus.CREATING_GROUP -> R.string.filter_creating_group
    }

private val ProjectStatus.label: Int
    get() = when (this) {
        ProjectStatus.PASSED -> R.string.status_passed
        ProjectStatus.FAILED -> R.string.status_failed
        ProjectStatus.IN_PROGRESS -> R.string.status_in_progress
        ProjectStatus.WAITING_FOR_CORRECTION -> R.string.status_waiting_for_correction
        ProjectStatus.SEARCHING_GROUP -> R.string.status_searching_group
        ProjectStatus.CREATING_GROUP -> R.string.status_creating_group
    }

private val ProjectSort.label: Int
    get() = when (this) {
        ProjectSort.DATE -> R.string.sort_date
        ProjectSort.GRADE -> R.string.sort_grade
        ProjectSort.NAME -> R.string.sort_name
    }

// Fixed colors: the coalition accent could itself be green or red.
private val ProjectStatus.color: Color
    get() = when (this) {
        ProjectStatus.PASSED -> Ok
        ProjectStatus.FAILED -> Bad
        ProjectStatus.WAITING_FOR_CORRECTION -> Violet
        ProjectStatus.IN_PROGRESS, ProjectStatus.SEARCHING_GROUP, ProjectStatus.CREATING_GROUP -> Amber
    }
