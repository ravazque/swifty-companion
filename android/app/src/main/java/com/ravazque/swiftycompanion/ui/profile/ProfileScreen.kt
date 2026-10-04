package com.ravazque.swiftycompanion.ui.profile

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.ravazque.swiftycompanion.model.ProjectStatus
import com.ravazque.swiftycompanion.ui.components.ErrorPanel
import com.ravazque.swiftycompanion.ui.profile.card.CARD_RATIO
import com.ravazque.swiftycompanion.ui.profile.card.ProfileCard
import com.ravazque.swiftycompanion.ui.profile.card.accentColor
import com.ravazque.swiftycompanion.ui.theme.SwiftyTheme
import kotlinx.coroutines.launch

// Below 600 dp one scrolling column with pinned tabs; from 600 dp (tablets, phones in landscape)
// the card and details on the left and the tabs on the right, each pane with its own scroll.
private val TwoPaneMinWidth = 600.dp
private val CardMinWidth = 300.dp
private val CardMaxWidth = 440.dp
private val TabsMaxWidth = 720.dp
private val SelectorHeight = 48.dp

class ProfileActions(
    val onRefresh: () -> Unit = {},
    val onFlipCard: () -> Unit = {},
    val onSelectCursus: (Int) -> Unit = {},
    val onSelectTab: (ProfileTab) -> Unit = {},
    val onSelectProjectFilter: (ProjectStatus?) -> Unit = {},
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
            onSelectTab = viewModel::selectTab,
            onSelectProjectFilter = viewModel::selectProjectFilter,
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
                    IconButton(onClick = actions.onRefresh, enabled = !state.loading) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.action_refresh))
                    }
                },
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val profile = state.profile
            when {
                profile != null -> ProfileBody(profile, state, actions)
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
private fun ProfileBody(profile: Profile, state: ProfileUiState, actions: ProfileActions) {
    val cursus = profile.cursusOrMain(state.selectedCursusId)
    val accent = profile.accentColor()
    // Created outside the layout switch, so each layout keeps its scroll when the window changes size.
    val columnList = rememberLazyListState()
    val sideScroll = rememberScrollState()
    val tabList = rememberLazyListState()
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Outside the scrolling content, so a failed refresh is visible wherever the lists are.
        state.error?.let { error ->
            ErrorPanel(
                error = error,
                onRetry = actions.onRefresh,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp).widthIn(max = CardMaxWidth),
            )
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            if (maxWidth >= TwoPaneMinWidth) {
                // As wide as fits the whole card in the pane's height, but never so narrow its text gets unreadable.
                val above = if (profile.cursus.size > 1) SelectorHeight + 16.dp else 0.dp
                val cardHeight = maxHeight - 32.dp - above
                val sideWidth = min(maxWidth * 0.4f, cardHeight * CARD_RATIO).coerceIn(CardMinWidth, CardMaxWidth)
                TwoPanes(profile, cursus, accent, state, actions, sideWidth, sideScroll, tabList)
            } else {
                OneColumn(profile, cursus, accent, state, actions, columnList)
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
    val scope = rememberCoroutineScope()
    // With the tabs pinned, a new tab starts at its top instead of wherever the old one was.
    val selectTab: (ProfileTab) -> Unit = { tab ->
        actions.onSelectTab(tab)
        val tabs = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == TABS_KEY }
        if (tabs != null && tabs.index < listState.firstVisibleItemIndex) {
            scope.launch { listState.scrollToItem(tabs.index) }
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "overview") { Overview(profile, cursus, accent, state, actions, column) }
        stickyHeader(key = TABS_KEY) {
            Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background), Alignment.Center) {
                ProfileTabs(state.tab, accent, selectTab, column)
            }
        }
        tabItems(profile, cursus, accent, state, actions, column)
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
    tabList: LazyListState,
) {
    val scope = rememberCoroutineScope()
    val selectTab: (ProfileTab) -> Unit = { tab ->
        actions.onSelectTab(tab)
        scope.launch { tabList.scrollToItem(0) }
    }
    Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Overview(
            profile = profile,
            cursus = cursus,
            accent = accent,
            state = state,
            actions = actions,
            modifier = Modifier.width(sideWidth).fillMaxHeight().verticalScroll(sideScroll).padding(vertical = 16.dp),
        )
        Column(Modifier.weight(1f).fillMaxHeight().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val content = Modifier.widthIn(max = TabsMaxWidth).fillMaxWidth()
            ProfileTabs(state.tab, accent, selectTab, content)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = tabList,
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                tabItems(profile, cursus, accent, state, actions, content)
            }
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
        DetailsSection(profile)
    }
}

private fun LazyListScope.tabItems(
    profile: Profile,
    cursus: Cursus?,
    accent: Color,
    state: ProfileUiState,
    actions: ProfileActions,
    modifier: Modifier,
) {
    when (state.tab) {
        ProfileTab.SKILLS -> item(key = "skills") { SkillsSection(cursus, accent, modifier) }
        ProfileTab.PROJECTS -> projectItems(profile, cursus?.id, state.projectFilter, accent, actions.onSelectProjectFilter, modifier)
    }
}

private const val TABS_KEY = "tabs"

@Preview(name = "Phone", widthDp = 411, heightDp = 891, showBackground = true, backgroundColor = 0xFF08090D)
@Preview(name = "Phone, landscape", widthDp = 891, heightDp = 411, showBackground = true, backgroundColor = 0xFF08090D)
@Preview(name = "Tablet", widthDp = 1280, heightDp = 800, showBackground = true, backgroundColor = 0xFF08090D)
@Composable
private fun ProfilePreview() {
    SwiftyTheme {
        ProfileContent("jdoe", ProfileUiState(profile = previewProfile), {}, ProfileActions())
    }
}

@Preview(name = "Projects", widthDp = 411, heightDp = 1600, showBackground = true, backgroundColor = 0xFF08090D)
@Preview(name = "Projects, tablet", widthDp = 1280, heightDp = 800, showBackground = true, backgroundColor = 0xFF08090D)
@Composable
private fun ProfileProjectsPreview() {
    SwiftyTheme {
        ProfileContent("jdoe", ProfileUiState(profile = previewProfile, tab = ProfileTab.PROJECTS), {}, ProfileActions())
    }
}
