package com.ravazque.swiftycompanion.ui.profile.card

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.ravazque.swiftycompanion.model.Skill
import com.ravazque.swiftycompanion.ui.theme.Muted
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Radar chart in the intra's style: five rings on the 0-21 skill scale, one spoke per skill
// with its name at the end, and the levels as a filled polygon. The radius is the largest one
// that keeps every label inside; labels that would still collide are pushed apart vertically.

private val Grid = Color.White.copy(alpha = 0.1f)

private class RadarLabel(val text: TextLayoutResult, val dir: Offset) {
    private val lines = 0 until text.lineCount
    val left = lines.minOf { text.getLineLeft(it) }
    val right = lines.maxOf { text.getLineRight(it) }
    val sideways = abs(dir.x) > 0.2f
    val stacked = abs(dir.y) > 0.25f
    var origin = Offset.Zero

    val bounds: Rect get() = Rect(origin.x + left, origin.y, origin.x + right, origin.y + text.size.height)

    fun extentX() = (right - left) * if (sideways) 1f else 0.5f
    fun extentY() = text.size.height * if (stacked) 1f else 0.5f

    fun placeAt(anchor: Offset) {
        val x = when {
            dir.x > 0.2f -> anchor.x - left
            dir.x < -0.2f -> anchor.x - right
            else -> anchor.x - (left + right) / 2
        }
        val y = when {
            dir.y < -0.25f -> anchor.y - text.size.height
            dir.y > 0.25f -> anchor.y
            else -> anchor.y - text.size.height / 2
        }
        origin = Offset(x, y)
    }
}

@Composable
internal fun SkillRadar(skills: List<Skill>, accent: Color, scale: CardScale, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val style = TextStyle(color = Muted, fontSize = scale.sp(0.82f), lineHeight = 1.15.em)
    val labelWidth = scale.dp(6f)
    val gap = scale.dp(0.6f)
    val strokeWidth = scale.dp(0.12f)
    Spacer(
        modifier.drawWithCache {
            val labels = skills.mapIndexed { i, skill ->
                val angle = 2 * PI * i / skills.size - PI / 2
                val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                val align = when {
                    dir.x > 0.2f -> TextAlign.Start
                    dir.x < -0.2f -> TextAlign.End
                    else -> TextAlign.Center
                }
                val text = measurer.measure(
                    skill.name,
                    style.copy(textAlign = align),
                    constraints = Constraints(maxWidth = labelWidth.roundToPx()),
                )
                RadarLabel(text, dir)
            }
            fun reach(half: Float, extent: Float, dir: Float) =
                if (abs(dir) < 1e-3f) Float.MAX_VALUE else (half - extent) / abs(dir)

            val labelRadius = labels.minOfOrNull {
                min(reach(size.width / 2, it.extentX(), it.dir.x), reach(size.height / 2, it.extentY(), it.dir.y))
            } ?: 0f
            val radius = (labelRadius - gap.toPx()).coerceAtLeast(0f)
            val center = Offset(size.width / 2, size.height / 2)
            labels.forEach { it.placeAt(center + it.dir * labelRadius) }

            // Labels on the vertical axis keep their place and push close side neighbors outwards.
            // Then, walking outwards from the center, each label moves past any placed one it hits.
            val pad = gap.toPx()
            labels.filterNot { it.sideways }.forEach { axis ->
                labels.filter { it.sideways && it.bounds.top < axis.bounds.bottom && axis.bounds.top < it.bounds.bottom }
                    .forEach { side ->
                        val dx = if (side.dir.x > 0) axis.bounds.right + pad - side.bounds.left else axis.bounds.left - pad - side.bounds.right
                        if (dx * side.dir.x > 0) side.origin += Offset(dx, 0f)
                    }
            }
            val spacing = pad / 3
            val placed = mutableListOf<Rect>()
            fun settle(group: List<RadarLabel>, down: Boolean) = group.forEach { label ->
                while (true) {
                    val box = label.bounds.let { it.copy(left = it.left - pad / 2, right = it.right + pad / 2) }
                    val hit = placed.firstOrNull { it.overlaps(box) } ?: break
                    val y = if (down) hit.bottom + spacing else hit.top - spacing - label.text.size.height
                    label.origin = label.origin.copy(y = y)
                }
                placed += label.bounds
            }
            settle(labels.filter { it.dir.y >= 0 }.sortedBy { it.bounds.top }, down = true)
            settle(labels.filter { it.dir.y < 0 }.sortedByDescending { it.bounds.bottom }, down = false)

            val shape = Path().apply {
                skills.forEachIndexed { i, skill ->
                    val point = center + labels[i].dir * (radius * skill.ratio.toFloat())
                    if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                }
                close()
            }
            val hairline = 1.dp.toPx()

            onDrawBehind {
                for (ring in 1..5) drawCircle(Grid, radius * ring / 5, center, style = Stroke(hairline))
                labels.forEach { drawLine(Grid, center, center + it.dir * radius, hairline) }
                drawPath(shape, accent.copy(alpha = 0.45f))
                drawPath(shape, accent, style = Stroke(strokeWidth.toPx(), join = StrokeJoin.Round))
                labels.forEach { drawText(it.text, topLeft = it.origin) }
            }
        },
    )
}
