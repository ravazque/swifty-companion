package com.ravazque.swiftycompanion.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ravazque.swiftycompanion.BuildConfig
import com.ravazque.swiftycompanion.R
import com.ravazque.swiftycompanion.SwiftyApp
import com.ravazque.swiftycompanion.data.auth.RenewalReason
import com.ravazque.swiftycompanion.data.auth.TokenInfo
import com.ravazque.swiftycompanion.data.auth.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// Debug builds only: how the access token is being used, plus two ways to force a renewal.
@Composable
fun TokenInspectorButton(modifier: Modifier = Modifier) {
    if (!BuildConfig.DEBUG) return
    var open by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = modifier) {
        Icon(Icons.Default.Lock, stringResource(R.string.inspector_title))
    }
    if (open) TokenInspectorSheet(onDismiss = { open = false })
}

class TokenInspectorViewModel(private val tokens: TokenManager) : ViewModel() {
    val info: StateFlow<TokenInfo> = tokens.info

    // TokenManager holds its lock during a token request, so it is never called from the main thread.
    init {
        onTokens { refreshInfo() }
    }

    fun expireNow() = onTokens { expireNow() }

    fun corrupt() = onTokens { corrupt() }

    private fun onTokens(action: TokenManager.() -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { tokens.action() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { TokenInspectorViewModel((this[APPLICATION_KEY] as SwiftyApp).container.tokens) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TokenInspectorSheet(
    onDismiss: () -> Unit,
    viewModel: TokenInspectorViewModel = viewModel(factory = TokenInspectorViewModel.Factory),
) {
    val info by viewModel.info.collectAsStateWithLifecycle()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1000)
            value = System.currentTimeMillis()
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.inspector_title), style = MaterialTheme.typography.titleLarge)
            InfoRow(stringResource(R.string.inspector_token), info.fingerprint ?: stringResource(R.string.inspector_none))
            InfoRow(stringResource(R.string.inspector_expires), expiresIn(info.expiresAt, now))
            InfoRow(stringResource(R.string.inspector_api_requests), info.apiRequests.toString())
            InfoRow(stringResource(R.string.inspector_token_requests), info.tokenRequests.toString())
            InfoRow(stringResource(R.string.inspector_last_renewal), lastRenewal(info))
            Text(
                text = stringResource(R.string.inspector_counters_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = viewModel::expireNow, enabled = info.fingerprint != null) {
                    Text(stringResource(R.string.inspector_expire))
                }
                OutlinedButton(onClick = viewModel::corrupt, enabled = info.fingerprint != null) {
                    Text(stringResource(R.string.inspector_corrupt))
                }
            }
            Text(
                text = stringResource(R.string.inspector_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun expiresIn(expiresAt: Long?, now: Long): String {
    if (expiresAt == null) return stringResource(R.string.inspector_none)
    val seconds = (expiresAt - now) / 1000
    if (seconds <= 0) return stringResource(R.string.inspector_expired)
    return String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
}

@Composable
private fun lastRenewal(info: TokenInfo): String {
    val renewal = info.lastRenewal ?: return stringResource(R.string.inspector_none)
    val locale = LocalConfiguration.current.locales[0]
    val times = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).withLocale(locale) }
    val time = times.format(Instant.ofEpochMilli(renewal.at).atZone(ZoneId.systemDefault()))
    return when (renewal.reason) {
        RenewalReason.PROACTIVE -> stringResource(R.string.inspector_reason_proactive, time)
        RenewalReason.AFTER_401 -> stringResource(R.string.inspector_reason_after_401, time)
    }
}
