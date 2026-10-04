package com.ravazque.swiftycompanion.ui.profile.card

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.ravazque.swiftycompanion.R
import com.ravazque.swiftycompanion.model.Skill
import com.ravazque.swiftycompanion.ui.theme.Muted
import com.ravazque.swiftycompanion.ui.theme.Surface2
import com.ravazque.swiftycompanion.ui.theme.TextMain
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Radar chart in the intra's style: five rings on the 0-21 skill scale, one spoke per skill
// with its name at the end, and the levels as a filled polygon. The radius is the largest one
// that keeps every label inside; labels that would still collide are pushed apart. Tapping a
// point or a name shows that skill's level and percentage in a bubble.

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

private class RadarLayout(val center: Offset, val radius: Float, val labels: List<RadarLabel>, val points: List<Offset>) {
    val shape = Path().apply {
        points.forEachIndexed { i, point -> if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y) }
        close()
    }

    // A name is the easiest target; a point counts when it is the closest one within reach.
    fun hit(position: Offset, reach: Float): Int? {
        labels.indexOfFirst { it.bounds.inflate(reach / 3).contains(position) }.takeIf { it >= 0 }?.let { return it }
        val nearest = points.indices.minByOrNull { (points[it] - position).getDistance() } ?: return null
        return nearest.takeIf { (points[it] - position).getDistance() <= reach }
    }
}

private fun radarLayout(
    measurer: TextMeasurer,
    skills: List<Skill>,
    halfStepBefore: Boolean,
    size: Size,
    style: TextStyle,
    labelWidth: Int,
    gap: Float,
): RadarLayout {
    val start = if (halfStepBefore) 0.5 else 0.0
    val labels = skills.mapIndexed { i, skill ->
        val angle = 2 * PI * (i - start) / skills.size - PI / 2
        val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
        val align = when {
            dir.x > 0.2f -> TextAlign.Start
            dir.x < -0.2f -> TextAlign.End
            else -> TextAlign.Center
        }
        RadarLabel(measurer.measure(skill.name, style.copy(textAlign = align), constraints = Constraints(maxWidth = labelWidth)), dir)
    }
    fun reach(half: Float, extent: Float, dir: Float) = if (abs(dir) < 1e-3f) Float.MAX_VALUE else (half - extent) / abs(dir)

    val labelRadius = labels.minOfOrNull {
        min(reach(size.width / 2, it.extentX(), it.dir.x), reach(size.height / 2, it.extentY(), it.dir.y))
    } ?: 0f
    val radius = (labelRadius - gap).coerceAtLeast(0f)
    val center = Offset(size.width / 2, size.height / 2)
    labels.forEach { it.placeAt(center + it.dir * labelRadius) }

    // Labels on the vertical axis keep their place and push close side neighbors outwards.
    // Then, walking outwards from the center, each label moves past any placed one it hits.
    labels.filterNot { it.sideways }.forEach { axis ->
        labels.filter { it.sideways && it.bounds.top < axis.bounds.bottom && axis.bounds.top < it.bounds.bottom }
            .forEach { side ->
                val dx = if (side.dir.x > 0) axis.bounds.right + gap - side.bounds.left else axis.bounds.left - gap - side.bounds.right
                if (dx * side.dir.x > 0) side.origin += Offset(dx, 0f)
            }
    }
    val spacing = gap / 3
    val placed = mutableListOf<Rect>()
    fun settle(group: List<RadarLabel>, down: Boolean) = group.forEach { label ->
        while (true) {
            val box = label.bounds.let { it.copy(left = it.left - gap / 2, right = it.right + gap / 2) }
            val hit = placed.firstOrNull { it.overlaps(box) } ?: break
            val y = if (down) hit.bottom + spacing else hit.top - spacing - label.text.size.height
            label.origin = label.origin.copy(y = y)
        }
        placed += label.bounds
    }
    settle(labels.filter { it.dir.y >= 0 }.sortedBy { it.bounds.top }, down = true)
    settle(labels.filter { it.dir.y < 0 }.sortedByDescending { it.bounds.bottom }, down = false)

    val points = skills.mapIndexed { i, skill -> center + labels[i].dir * (radius * skill.ratio.toFloat()) }
    return RadarLayout(center, radius, labels, points)
}

@Composable
internal fun SkillRadar(
    skills: List<Skill>,
    halfStepBefore: Boolean,
    accent: Color,
    scale: CardScale,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val locale = LocalConfiguration.current.locales[0]
    val style = TextStyle(color = Muted, fontSize = scale.sp(0.82f), lineHeight = 1.15.em)
    var selected by remember(skills) { mutableStateOf<Int?>(null) }
    val values = skills.map {
        stringResource(R.string.radar_value, String.format(locale, "%.2f", it.level), String.format(locale, "%.2f", it.percent))
    }
    val description = skills.indices.joinToString("; ") { "${skills[it].name}: ${values[it]}" }

    BoxWithConstraints(modifier.semantics { contentDescription = description }) {
        val size = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val unit = scale.dp(1f)
        val layout = remember(skills, halfStepBefore, size, unit, density) {
            with(density) { radarLayout(measurer, skills, halfStepBefore, size, style, (unit * 6).roundToPx(), (unit * 0.6f).toPx()) }
        }
        val bubble = selected?.let { i ->
            val text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(skills[i].name) }
                append("\n")
                append(values[i])
            }
            measurer.measure(
                text,
                TextStyle(color = TextMain, fontSize = scale.sp(0.9f), lineHeight = 1.3.em, textAlign = TextAlign.Center),
                constraints = Constraints(maxWidth = (size.width * 0.7f).toInt()),
            )
        }
        Spacer(
            Modifier
                .fillMaxSize()
                .pointerInput(layout) {
                    val reach = 24.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val hit = layout.hit(down.position, reach)
                        // Nothing hit and no bubble open: the tap goes on to the card, which flips.
                        if (hit == null && selected == null) return@awaitEachGesture
                        down.consume()
                        val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                        up.consume()
                        selected = if (hit == selected) null else hit
                    }
                }
                .drawBehind {
                    val hairline = 1.dp.toPx()
                    val dot = unit.toPx() * 0.2f
                    with(layout) {
                        for (ring in 1..5) drawCircle(Grid, radius * ring / 5, center, style = Stroke(hairline))
                        labels.forEach { drawLine(Grid, center, center + it.dir * radius, hairline) }
                        drawPath(shape, accent.copy(alpha = 0.45f))
                        drawPath(shape, accent, style = Stroke(unit.toPx() * 0.12f, join = StrokeJoin.Round))
                        points.forEach { drawCircle(accent, dot, it) }
                        labels.forEachIndexed { i, label ->
                            drawText(label.text, color = if (i == selected) accent else Color.Unspecified, topLeft = label.origin)
                        }
                    }
                    val index = selected ?: return@drawBehind
                    val text = bubble ?: return@drawBehind
                    val point = layout.points[index]
                    drawCircle(accent, dot * 2.2f, point)
                    drawCircle(TextMain, dot * 2.2f, point, style = Stroke(hairline * 1.5f))
                    // Above the point if it fits, below otherwise, and always inside the chart.
                    val pad = unit.toPx() * 0.6f
                    val box = Size(text.size.width + pad * 2, text.size.height + pad * 1.4f)
                    val above = point.y - dot * 3 - box.height
                    val top = (if (above >= 0f) above else point.y + dot * 3).coerceIn(0f, (size.height - box.height).coerceAtLeast(0f))
                    val left = (point.x - box.width / 2).coerceIn(0f, (size.width - box.width).coerceAtLeast(0f))
                    val corner = CornerRadius(pad, pad)
                    drawRoundRect(Surface2, Offset(left, top), box, corner)
                    drawRoundRect(accent, Offset(left, top), box, corner, style = Stroke(hairline))
                    drawText(text, topLeft = Offset(left + pad, top + pad * 0.7f))
                },
        )
    }
}
