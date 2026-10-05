package com.ravazque.swiftycompanion.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ravazque.swiftycompanion.R
import com.ravazque.swiftycompanion.model.AppError
import com.ravazque.swiftycompanion.model.Visibility
import com.ravazque.swiftycompanion.ui.components.ErrorPanel
import com.ravazque.swiftycompanion.ui.components.LanguageSwitch
import com.ravazque.swiftycompanion.ui.components.isInputError
import com.ravazque.swiftycompanion.ui.components.message
import com.ravazque.swiftycompanion.ui.debug.TokenInspectorButton
import com.ravazque.swiftycompanion.ui.theme.SwiftyTheme
import kotlinx.coroutines.launch

class SearchActions(
    val onQueryChange: (String) -> Unit = {},
    val onSearch: () -> Unit = {},
    val onShowStaff: (Boolean) -> Unit = {},
    val onShowBlackholed: (Boolean) -> Unit = {},
    val onShowFrozen: (Boolean) -> Unit = {},
    val onSignOut: () -> Unit = {},
    val onOpenTerms: () -> Unit = {},
    val onOpenPrivacy: () -> Unit = {},
)

@Composable
fun SearchScreen(
    onProfileFound: (String) -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val currentOnProfileFound by rememberUpdatedState(onProfileFound)
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { viewModel.found.collect { currentOnProfileFound(it) } }
            launch { viewModel.openLogin.collect { url -> if (!openSignInPage(context, url)) viewModel.browserMissing() } }
            launch { viewModel.redirects.collect(viewModel::completeLogin) }
        }
    }

    val actions = remember(viewModel, onOpenTerms, onOpenPrivacy) {
        SearchActions(
            onQueryChange = viewModel::onQueryChange,
            onSearch = viewModel::search,
            onShowStaff = viewModel::showStaff,
            onShowBlackholed = viewModel::showBlackholed,
            onShowFrozen = viewModel::showFrozen,
            onSignOut = viewModel::signOut,
            onOpenTerms = onOpenTerms,
            onOpenPrivacy = onOpenPrivacy,
        )
    }
    SearchContent(state, account, actions)
    if (state.consentOpen) {
        ConsentDialog(
            onAccept = viewModel::signIn,
            onDismiss = viewModel::dismissConsent,
            onOpenTerms = onOpenTerms,
            onOpenPrivacy = onOpenPrivacy,
        )
    }
}

@Composable
fun SearchContent(state: SearchUiState, account: String?, actions: SearchActions) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            // min height = viewport keeps the form centered while still scrolling on short screens.
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Column(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                    SearchForm(state, actions)
                }
            }
            LanguageSwitch(Modifier.align(Alignment.TopStart).padding(8.dp))
            Row(Modifier.align(Alignment.TopEnd).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                account?.let { AccountMenu(it, actions.onSignOut) }
                TokenInspectorButton()
            }
        }
    }
}

@Composable
private fun SearchForm(state: SearchUiState, actions: SearchActions) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val submit = {
        keyboard?.hide()
        focusManager.clearFocus()
        actions.onSearch()
    }
    val inputError = state.error?.takeIf { it.isInputError }
    val panelError = state.error?.takeUnless { it.isInputError }

    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.search_subtitle),
        style = MaterialTheme.typography.labelLarge,
        letterSpacing = 0.12.em,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(28.dp))

    OutlinedTextField(
        value = state.query,
        onValueChange = actions.onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.search_label)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (state.query.isNotEmpty()) {
            {
                IconButton(onClick = { actions.onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_clear))
                }
            }
        } else {
            null
        },
        isError = inputError != null,
        supportingText = inputError?.let { error ->
            { Text(error.message(), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(onSearch = { submit() }),
    )
    Spacer(Modifier.height(12.dp))

    Button(
        onClick = submit,
        enabled = !state.loading,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        if (state.loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(stringResource(R.string.search_button))
        }
    }

    Spacer(Modifier.height(12.dp))
    VisibilityOptions(state.visibility, actions)

    if (panelError != null) {
        Spacer(Modifier.height(16.dp))
        ErrorPanel(panelError, onRetry = actions.onSearch)
    }

    Spacer(Modifier.height(20.dp))
    LegalLinks(actions)
}

@Composable
private fun LegalLinks(actions: SearchActions) {
    val colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        TextButton(onClick = actions.onOpenPrivacy, colors = colors) { Text(stringResource(R.string.legal_privacy_title)) }
        TextButton(onClick = actions.onOpenTerms, colors = colors) { Text(stringResource(R.string.legal_terms_title)) }
    }
}

// Staff, blackholed and frozen profiles are only shown when their option is on.
@Composable
private fun VisibilityOptions(visibility: Visibility, actions: SearchActions) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        OptionChip(stringResource(R.string.card_staff), visibility.staff, actions.onShowStaff)
        OptionChip(stringResource(R.string.card_blackholed), visibility.blackholed, actions.onShowBlackholed)
        OptionChip(stringResource(R.string.card_frozen), visibility.frozen, actions.onShowFrozen)
    }
}

@Composable
private fun OptionChip(label: String, selected: Boolean, onChange: (Boolean) -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { onChange(!selected) },
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
            selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF08090D)
@Composable
private fun SearchPreview() {
    SwiftyTheme {
        SearchContent(SearchUiState(query = "jdoe", error = AppError.NotFound("jdoe"), visibility = Visibility(staff = true)), "ravazque", SearchActions())
    }
}
