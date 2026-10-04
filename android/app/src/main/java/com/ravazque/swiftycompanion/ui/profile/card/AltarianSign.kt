package com.ravazque.swiftycompanion.ui.profile.card

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// The wallet currency sign (₳, an A crossed by two bars), drawn as a vector: many phone fonts
// have no glyph for it and show an empty box instead.
internal val AltarianSign: ImageVector = ImageVector.Builder(
    name = "AltarianSign",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).path(
    stroke = SolidColor(Color.Black),
    strokeLineWidth = 2.2f,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
) {
    moveTo(4.5f, 21f)
    lineTo(12f, 3f)
    lineTo(19.5f, 21f)
    moveTo(3.5f, 12.5f)
    lineTo(20.5f, 12.5f)
    moveTo(3.5f, 16.5f)
    lineTo(20.5f, 16.5f)
}.build()
