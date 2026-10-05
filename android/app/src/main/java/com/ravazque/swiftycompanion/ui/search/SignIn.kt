package com.ravazque.swiftycompanion.ui.search

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.ravazque.swiftycompanion.R
import com.ravazque.swiftycompanion.ui.theme.Ink

// The intra page opens in a Custom Tab: the real site with its address, never a WebView whose
// content the app could read (RFC 8252). False when the device has no browser at all.
fun openSignInPage(context: Context, url: String): Boolean = try {
    CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(Ink.toArgb()).build())
        .build()
        .launchUrl(context, url.toUri())
    true
} catch (_: ActivityNotFoundException) {
    false
}

// Signing in needs an explicit acceptance: the box starts unticked every time the dialog opens. The
// documents open from their own buttons, so touching the sentence only ticks the box.
@Composable
fun ConsentDialog(onAccept: () -> Unit, onDismiss: () -> Unit, onOpenTerms: () -> Unit, onOpenPrivacy: () -> Unit) {
    var accepted by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text(stringResource(R.string.consent_title)) },
        text = {
            // Scrolls when the screen is short (a phone in landscape), so the box and its sentence stay reachable.
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.consent_body))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onOpenTerms) { Text(stringResource(R.string.legal_terms_title)) }
                    TextButton(onClick = onOpenPrivacy) { Text(stringResource(R.string.legal_privacy_title)) }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = accepted, role = Role.Checkbox, onValueChange = { accepted = it }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = accepted, onCheckedChange = null)
                    Text(stringResource(R.string.consent_check), Modifier.padding(start = 12.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = onAccept, enabled = accepted) { Text(stringResource(R.string.consent_accept)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
fun AccountMenu(login: String, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        TextButton(onClick = { open = true }) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(login, Modifier.padding(start = 6.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Text(
                text = stringResource(R.string.account_signed_in, login),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.account_sign_out)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                onClick = {
                    open = false
                    onSignOut()
                },
            )
        }
    }
}
