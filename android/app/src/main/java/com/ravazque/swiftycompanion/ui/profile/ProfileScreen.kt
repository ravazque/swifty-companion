package com.ravazque.swiftycompanion.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravazque.swiftycompanion.R
import com.ravazque.swiftycompanion.model.Profile
import com.ravazque.swiftycompanion.ui.components.ErrorPanel
import com.ravazque.swiftycompanion.ui.profile.card.ProfileCard
import com.ravazque.swiftycompanion.ui.profile.card.accentColor
import com.ravazque.swiftycompanion.ui.theme.SwiftyTheme

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
        onSelectCursus = viewModel::selectCursus,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    login: String,
    state: ProfileUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSelectCursus: (Int) -> Unit,
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
                profile != null -> ProfileBody(profile, state, onRefresh, onSelectCursus)
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
private fun ProfileBody(
    profile: Profile,
    state: ProfileUiState,
    onRetry: () -> Unit,
    onSelectCursus: (Int) -> Unit,
) {
    val cursus = profile.cursusOrMain(state.selectedCursusId)
    val accent = profile.accentColor()
    val column = Modifier.widthIn(max = 440.dp).fillMaxWidth()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Stable keys keep each item's saved state (like the card's side) when others come and go.
        state.error?.let { error -> item(key = "error") { ErrorPanel(error, onRetry, column) } }
        if (profile.cursus.size > 1) {
            item(key = "cursus") { CursusSelector(profile.cursus, cursus?.id, accent, onSelectCursus, column) }
        }
        item(key = "card") { ProfileCard(profile, cursus, column) }
        cursus?.let { item(key = "level") { LevelBlock(it, accent, column) } }
        item(key = "details") { DetailsSection(profile, column) }
        item(key = "skills") { SkillsSection(cursus, accent, column) }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090D, heightDp = 1600)
@Composable
private fun ProfilePreview() {
    SwiftyTheme {
        ProfileContent("jdoe", ProfileUiState(profile = previewProfile), {}, {}, {})
    }
}
