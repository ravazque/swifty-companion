package com.ravazque.swiftycompanion.ui.components

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.edit
import com.ravazque.swiftycompanion.R
import java.util.Locale

// The app's language, chosen inside the app. Android 13+ keeps it in the system's per-app
// language setting (which also changes it from the system settings); older versions keep it in
// preferences and MainActivity applies it when it is created.
object AppLanguage {
    private const val PREFS = "settings"
    private const val KEY = "language"

    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return base
        val config = Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
        return base.createConfigurationContext(config)
    }

    fun set(activity: Activity, tag: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(tag)
        } else {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY, tag) }
            activity.recreate()
        }
    }
}

// "EN | ES": the current language is highlighted, the other one switches to it.
@Composable
fun LanguageSwitch(modifier: Modifier = Modifier) {
    val activity = LocalActivity.current ?: return
    val spanish = LocalConfiguration.current.locales[0].language == "es"
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        LanguageOption("EN", stringResource(R.string.language_english), current = !spanish) { AppLanguage.set(activity, "en") }
        Text("|", color = MaterialTheme.colorScheme.outline)
        LanguageOption("ES", stringResource(R.string.language_spanish), current = spanish) { AppLanguage.set(activity, "es") }
    }
}

@Composable
private fun LanguageOption(code: String, name: String, current: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = !current, modifier = Modifier.semantics { contentDescription = name }) {
        Text(
            text = code,
            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
            color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
