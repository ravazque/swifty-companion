package com.ravazque.swiftycompanion.ui.legal

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ravazque.swiftycompanion.R
import com.ravazque.swiftycompanion.ui.theme.SwiftyTheme

// The terms of use and the privacy policy, in the app's language. Changing either text means raising
// TERMS_VERSION, so everyone accepts the new one before searching again.
enum class LegalDocument(@param:StringRes val title: Int, val sections: List<Pair<Int, Int>>) {
    TERMS(
        R.string.legal_terms_title,
        listOf(
            R.string.terms_app_title to R.string.terms_app_body,
            R.string.terms_who_title to R.string.terms_who_body,
            R.string.terms_use_title to R.string.terms_use_body,
            R.string.terms_data_title to R.string.terms_data_body,
            R.string.terms_warranty_title to R.string.terms_warranty_body,
            R.string.terms_stop_title to R.string.terms_stop_body,
            R.string.terms_changes_title to R.string.terms_changes_body,
        ),
    ),
    PRIVACY(
        R.string.legal_privacy_title,
        listOf(
            R.string.privacy_who_title to R.string.privacy_who_body,
            R.string.privacy_sign_in_title to R.string.privacy_sign_in_body,
            R.string.privacy_profiles_title to R.string.privacy_profiles_body,
            R.string.privacy_device_title to R.string.privacy_device_body,
            R.string.privacy_not_title to R.string.privacy_not_body,
            R.string.privacy_why_title to R.string.privacy_why_body,
            R.string.privacy_choices_title to R.string.privacy_choices_body,
            R.string.privacy_changes_title to R.string.privacy_changes_body,
        ),
    ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(document: LegalDocument, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(document.title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    text = stringResource(R.string.legal_updated),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                document.sections.forEach { (title, body) ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF08090D, heightDp = 1400)
@Composable
private fun PrivacyPreview() {
    SwiftyTheme { LegalScreen(LegalDocument.PRIVACY, onBack = {}) }
}
