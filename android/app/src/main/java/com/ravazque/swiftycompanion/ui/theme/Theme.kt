package com.ravazque.swiftycompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = DefaultAccent,
    onPrimary = Ink,
    background = Ink,
    onBackground = TextMain,
    surface = Ink,
    onSurface = TextMain,
    surfaceVariant = Surface2,
    onSurfaceVariant = Muted,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = Surface1,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Surface2,
    outline = Line,
    outlineVariant = LineSoft,
    error = Bad,
    onError = Ink,
    errorContainer = Color(0xFF2A1518),
    onErrorContainer = Color(0xFFFFB4B4),
)

@Composable
fun SwiftyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
