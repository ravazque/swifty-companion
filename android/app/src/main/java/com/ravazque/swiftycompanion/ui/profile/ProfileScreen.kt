package com.ravazque.swiftycompanion.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.ravazque.swiftycompanion.R
import com.ravazque.swiftycompanion.model.Coalition
import com.ravazque.swiftycompanion.model.Cursus
import com.ravazque.swiftycompanion.model.Profile
import com.ravazque.swiftycompanion.ui.components.ErrorPanel
import com.ravazque.swiftycompanion.ui.theme.DefaultAccent
import com.ravazque.swiftycompanion.ui.theme.LineSoft
import com.ravazque.swiftycompanion.ui.theme.Ok
import com.ravazque.swiftycompanion.ui.theme.Surface1
import com.ravazque.swiftycompanion.ui.theme.Surface2
import com.ravazque.swiftycompanion.ui.theme.SwiftyTheme
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileContent(
        login = viewModel.login,
        state = state,
        onBack = onBack,
        onRefresh = viewModel::load,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    login: String,
    state: ProfileUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(login, fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !state.loading) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.action_refresh))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val profile = state.profile
            when {
                profile != null -> ProfileBody(profile, state, onRefresh)
                state.error != null -> ErrorPanel(
                    error = state.error,
                    onRetry = onRefresh,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp).widthIn(max = 480.dp),
                )
                else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            if (state.loading && profile != null) {
                LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun ProfileBody(profile: Profile, state: ProfileUiState, onRetry: () -> Unit) {
    val column = Modifier.widthIn(max = 640.dp)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        state.error?.let { error -> item { ErrorPanel(error, onRetry, column) } }
        item { ProfileHeader(profile, column) }
        item { DetailsSection(profile, column) }
    }
}

@Composable
private fun ProfileHeader(profile: Profile, modifier: Modifier) {
    val accent = profile.accentColor()
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = profile.imageUrl,
            contentDescription = profile.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Surface2)
                .border(3.dp, accent, CircleShape),
        )
        Spacer(Modifier.height(12.dp))
        Text(profile.displayName, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            text = "@${profile.login}",
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        profile.title?.let { Text(it, color = accent, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center) }
        profile.mainCursus?.let { cursus ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.profile_level, cursus.levelNumber, cursus.levelPercent),
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = listOfNotNull(cursus.name, cursus.grade).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (profile.isStaff) {
            Text(stringResource(R.string.profile_staff), color = accent, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun DetailsSection(profile: Profile, modifier: Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val unavailable = stringResource(R.string.profile_unavailable)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Surface1,
        border = BorderStroke(1.dp, LineSoft),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            DetailRow(stringResource(R.string.profile_email), profile.email ?: unavailable)
            DetailRow(stringResource(R.string.profile_phone), profile.phone ?: stringResource(R.string.profile_hidden))
            DetailRow(
                label = stringResource(R.string.profile_location),
                value = profile.location ?: unavailable,
                valueColor = if (profile.location != null) Ok else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DetailRow(stringResource(R.string.profile_wallet), "${profile.wallet} ₳")
            DetailRow(stringResource(R.string.profile_correction_points), profile.correctionPoints.toString())
            DetailRow(stringResource(R.string.profile_campus), profile.campus ?: unavailable)
            DetailRow(stringResource(R.string.profile_pool), profile.pool?.format(locale) ?: unavailable)
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
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
            color = valueColor,
            textAlign = TextAlign.End,
        )
    }
}

fun Profile.accentColor(): Color =
    coalition?.color?.let { runCatching { Color(it.toColorInt()) }.getOrNull() } ?: DefaultAccent

private fun YearMonth.format(locale: Locale): String =
    DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(this).replaceFirstChar { it.titlecase(locale) }

private val previewProfile = Profile(
    login = "jdoe",
    displayName = "John Doe",
    imageUrl = null,
    email = "jdoe@example.com",
    phone = null,
    location = "c1r2s3",
    wallet = 120,
    correctionPoints = 5,
    pool = YearMonth.of(2024, 7),
    campus = "North",
    title = "Mastermind jdoe",
    isStaff = false,
    cursus = listOf(Cursus(21, "Main cursus", "main", 7.42, "Member", emptyList())),
    projects = emptyList(),
    coalition = Coalition("Blue", "#3F8EFC", null),
)

@Preview(showBackground = true, backgroundColor = 0xFF08090D)
@Composable
private fun ProfilePreview() {
    SwiftyTheme {
        ProfileContent("jdoe", ProfileUiState(profile = previewProfile), {}, {})
    }
}
