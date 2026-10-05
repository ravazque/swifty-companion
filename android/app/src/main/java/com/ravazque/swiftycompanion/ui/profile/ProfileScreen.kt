package com.ravazque.swiftycompanion.ui.profile

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravazque.swiftycompanion.R
import com.ravazque.swiftycompanion.model.Cursus
import com.ravazque.swiftycompanion.model.Profile
import com.ravazque.swiftycompanion.model.ProjectSort
import com.ravazque.swiftycompanion.model.ProjectStatus
import com.ravazque.swiftycompanion.ui.components.ErrorPanel
import com.ravazque.swiftycompanion.ui.debug.TokenInspectorButton
import com.ravazque.swiftycompanion.ui.profile.card.CARD_RATIO
import com.ravazque.swiftycompanion.ui.profile.card.ProfileCard
import com.ravazque.swiftycompanion.ui.profile.card.accentFor
import com.ravazque.swiftycompanion.ui.theme.SwiftyTheme

// Below 600 dp one scrolling column; from 600 dp (tablets, phones in landscape) the card and
// details on the left and the projects on the right, each pane with its own scroll. On wide
// screens both panes stay together in the middle, so no empty strip belongs to the project list.
private val TwoPaneMinWidth = 600.dp
private val PaneGap = 24.dp
private val PanePadding = 16.dp
private val CardMinWidth = 300.dp
private val CardMaxWidth = 440.dp
private val ListMaxWidth = 720.dp
private val SelectorHeight = 48.dp

class ProfileActions(
    val onRefresh: () -> Unit = {},
    val onFlipCard: () -> Unit = {},
    val onSelectCursus: (Int) -> Unit = {},
    val onSelectProjectFilter: (ProjectStatus?) -> Unit = {},
    val onSelectProjectSort: (ProjectSort) -> Unit = {},
)

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        ProfileActions(
            onRefresh = viewModel::load,
            onFlipCard = viewModel::flipCard,
            onSelectCursus = viewModel::selectCursus,
            onSelectProjectFilter = viewModel::selectProjectFilter,
            onSelectProjectSort = viewModel::selectProjectSort,
        )
    }
    ProfileContent(viewModel.login, state, onBack, actions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    login: String,
    state: ProfileUiState,
    onBack: () -> Unit,
    actions: ProfileActions,
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(login, fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    TokenInspectorButton()
                    IconButton(onClick = actions.onRefresh, enabled = !state.loading) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.action_refresh))
                    }
                },
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                colors = TopAppBarDefaults.topAppBarColors(scrolledContainerColor = MaterialTheme.colorScheme.background),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val profile = state.profile
            when {
                profile != null -> ProfileBody(profile, state, actions, topBarOffset = { scrollBehavior.state.heightOffset })
                state.error != null -> ErrorPanel(
                    error = state.error,
                    onRetry = actions.onRefresh,
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
private fun ProfileBody(profile: Profile, state: ProfileUiState, actions: ProfileActions, topBarOffset: () -> Float) {
    val cursus = profile.cursusOrMain(state.selectedCursusId)
    val accent = profile.accentFor(cursus)
    // Created outside the layout switch, so each layout keeps its scroll when the window changes size.
    val columnList = rememberLazyListState()
    val sideScroll = rememberScrollState()
    val projectList = rememberLazyListState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // The height with the top bar fully shown (its offset is negative while it scrolls away), so the
        // card keeps its size while the bar moves. The side pane is as wide as fits the whole card in
        // that height, but never so narrow that the card's text gets unreadable.
        val height = maxHeight + with(LocalDensity.current) { topBarOffset().toDp() }
        val above = if (profile.cursus.size > 1) SelectorHeight + 16.dp else 0.dp
        val sideWidth = min(maxWidth * 0.4f, (height - 32.dp - above) * CARD_RATIO).coerceIn(CardMinWidth, CardMaxWidth)
        val twoPanes = maxWidth >= TwoPaneMinWidth
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            state.error?.let { error ->
                ErrorPanel(
                    error = error,
                    onRetry = actions.onRefresh,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp).widthIn(max = CardMaxWidth),
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                if (twoPanes) {
                    TwoPanes(profile, cursus, accent, state, actions, sideWidth, sideScroll, projectList)
                } else {
                    OneColumn(profile, cursus, accent, state, actions, columnList)
                }
            }
        }
    }
}

@Composable
private fun OneColumn(
    profile: Profile,
    cursus: Cursus?,
    accent: Color,
    state: ProfileUiState,
    actions: ProfileActions,
    listState: LazyListState,
) {
    val column = Modifier.widthIn(max = CardMaxWidth).fillMaxWidth()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "overview") { Overview(profile, cursus, accent, state, actions, column) }
        projects(profile, cursus, accent, state, actions, column)
    }
}

@Composable
private fun TwoPanes(
    profile: Profile,
    cursus: Cursus?,
    accent: Color,
    state: ProfileUiState,
    actions: ProfileActions,
    sideWidth: Dp,
    sideScroll: ScrollState,
    listState: LazyListState,
) {
    Row(
        modifier = Modifier
            .widthIn(max = sideWidth + PaneGap + ListMaxWidth + PanePadding * 2)
            .fillMaxSize()
            .padding(horizontal = PanePadding),
        horizontalArrangement = Arrangement.spacedBy(PaneGap),
    ) {
        Overview(
            profile = profile,
            cursus = cursus,
            accent = accent,
            state = state,
            actions = actions,
            modifier = Modifier.width(sideWidth).fillMaxHeight().verticalScroll(sideScroll).padding(vertical = 16.dp),
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            state = listState,
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            projects(profile, cursus, accent, state, actions, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Overview(
    profile: Profile,
    cursus: Cursus?,
    accent: Color,
    state: ProfileUiState,
    actions: ProfileActions,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (profile.cursus.size > 1) CursusSelector(profile.cursus, cursus?.id, accent, actions.onSelectCursus)
        ProfileCard(profile, cursus, state.cardFlipped, actions.onFlipCard, Modifier.fillMaxWidth())
        cursus?.let { LevelBlock(it, accent) }
        DetailsSection(profile, cursus)
    }
}

private fun LazyListScope.projects(
    profile: Profile,
    cursus: Cursus?,
    accent: Color,
    state: ProfileUiState,
    actions: ProfileActions,
    modifier: Modifier,
) = projectItems(
    projects = profile.projectsOf(cursus),
    filter = state.projectView.filter,
    sort = state.projectView.sort,
    accent = accent,
    onSelectFilter = actions.onSelectProjectFilter,
    onSelectSort = actions.onSelectProjectSort,
    modifier = modifier,
)

@Preview(name = "Phone", widthDp = 411, heightDp = 1400, showBackground = true, backgroundColor = 0xFF08090D)
@Preview(name = "Phone, landscape", widthDp = 891, heightDp = 411, showBackground = true, backgroundColor = 0xFF08090D)
@Preview(name = "Tablet", widthDp = 1280, heightDp = 800, showBackground = true, backgroundColor = 0xFF08090D)
@Composable
private fun ProfilePreview() {
    SwiftyTheme {
        ProfileContent("jdoe", ProfileUiState(profile = previewProfile), {}, ProfileActions())
    }
}

@Preview(name = "Piscine", widthDp = 411, heightDp = 1400, showBackground = true, backgroundColor = 0xFF08090D)
@Composable
private fun ProfilePiscinePreview() {
    SwiftyTheme {
        ProfileContent("jdoe", ProfileUiState(profile = previewProfile, selectedCursusId = 9), {}, ProfileActions())
    }
}
