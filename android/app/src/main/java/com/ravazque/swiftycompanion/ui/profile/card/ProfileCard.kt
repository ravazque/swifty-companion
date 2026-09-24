package com.ravazque.swiftycompanion.ui.profile.card

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.graphics.toColorInt
import coil3.compose.AsyncImage
import com.ravazque.swiftycompanion.R
import com.ravazque.swiftycompanion.model.Cursus
import com.ravazque.swiftycompanion.model.Profile
import com.ravazque.swiftycompanion.ui.profile.previewProfile
import com.ravazque.swiftycompanion.ui.theme.CardBottom
import com.ravazque.swiftycompanion.ui.theme.DefaultAccent
import com.ravazque.swiftycompanion.ui.theme.Faint
import com.ravazque.swiftycompanion.ui.theme.Line
import com.ravazque.swiftycompanion.ui.theme.LineSoft
import com.ravazque.swiftycompanion.ui.theme.Muted
import com.ravazque.swiftycompanion.ui.theme.Ok
import com.ravazque.swiftycompanion.ui.theme.Surface1
import com.ravazque.swiftycompanion.ui.theme.Surface2
import com.ravazque.swiftycompanion.ui.theme.SwiftyTheme
import com.ravazque.swiftycompanion.ui.theme.TextMain
import java.util.Locale

// Front of the profile card. Every size is a multiple of one unit (card width / 32), so the card
// scales as a single piece on any screen; its text deliberately ignores the system font scale.

private const val CARD_RATIO = 5f / 7f

@Immutable
private class CardScale(private val unit: Dp, private val density: Density) {
    fun dp(n: Float): Dp = unit * n
    fun sp(n: Float): TextUnit = with(density) { (unit * n).toSp() }
}

@Composable
fun ProfileCard(profile: Profile, cursus: Cursus?, modifier: Modifier = Modifier) {
    val accent = profile.accentColor()
    BoxWithConstraints(modifier.aspectRatio(CARD_RATIO)) {
        val scale = CardScale(maxWidth / 32, LocalDensity.current)
        val shape = RoundedCornerShape(scale.dp(1.5f))
        ProvideTextStyle(TextStyle(color = TextMain, lineHeight = 1.3.em)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .drawWithCache {
                        val brush = Brush.linearGradient(
                            0f to lerp(Surface2, accent, 0.14f),
                            0.45f to Surface1,
                            1f to CardBottom,
                            start = Offset.Zero,
                            end = Offset(size.width * 0.36f, size.height),
                        )
                        onDrawBehind { drawRect(brush) }
                    }
                    .border(1.dp, Line, shape)
                    .padding(scale.dp(1.7f)),
            ) {
                CardHead(profile, cursus, accent, scale)
                Portrait(profile, cursus, accent, scale, Modifier.padding(top = scale.dp(1.4f)))
                Spacer(Modifier.weight(1f))
                Identity(profile, cursus, accent, scale)
                Spacer(Modifier.weight(1f))
                Stats(profile, cursus, scale)
                Spacer(Modifier.height(scale.dp(1.2f)))
                LocationTag(profile.location, scale)
            }
        }
    }
}

fun Profile.accentColor(): Color =
    coalition?.color?.let { runCatching { Color(it.toColorInt()) }.getOrNull() } ?: DefaultAccent

@Composable
private fun CardHead(profile: Profile, cursus: Cursus?, accent: Color, scale: CardScale) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(scale.dp(0.6f)),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(scale.dp(0.6f)),
        ) {
            profile.coalition?.let { coalition ->
                AsyncImage(
                    model = coalition.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(scale.dp(2f)),
                )
                Text(
                    text = coalition.name.uppercase(),
                    color = accent,
                    fontSize = scale.sp(0.95f),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.08.em,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (profile.isStaff) Tag(stringResource(R.string.profile_staff), accent, scale)
        cursus?.let { Tag(stringResource(R.string.card_level_tag, it.levelNumber), Faint, scale) }
    }
}

@Composable
private fun Tag(text: String, color: Color, scale: CardScale) {
    Text(
        text = text,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = scale.sp(0.95f),
        maxLines = 1,
        modifier = Modifier
            .border(1.dp, Line, CircleShape)
            .padding(horizontal = scale.dp(0.8f), vertical = scale.dp(0.15f)),
    )
}

@Composable
private fun Portrait(profile: Profile, cursus: Cursus?, accent: Color, scale: CardScale, modifier: Modifier) {
    Box(modifier.fillMaxWidth().height(scale.dp(15f)), contentAlignment = Alignment.Center) {
        LevelRing(
            progress = (cursus?.levelPercent ?: 0) / 100f,
            color = accent,
            strokeWidth = scale.dp(0.55f),
            modifier = Modifier.size(scale.dp(15f)),
        )
        Box(
            modifier = Modifier
                .size(scale.dp(11.8f))
                .clip(CircleShape)
                .background(Surface2)
                .border(scale.dp(0.17f), accent.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Drawn underneath the photo: visible only while it loads or when there is none.
            Text(profile.initials(), color = Muted, fontSize = scale.sp(3.6f))
            AsyncImage(
                model = profile.imageUrl,
                contentDescription = profile.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun Identity(profile: Profile, cursus: Cursus?, accent: Color, scale: CardScale) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = profile.displayName,
            fontSize = scale.sp(1.9f),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "@${profile.login}",
            color = Muted,
            fontFamily = FontFamily.Monospace,
            fontSize = scale.sp(1.12f),
        )
        cursus?.let {
            Text(
                text = listOfNotNull(it.grade, it.name).joinToString(" · "),
                color = Faint,
                fontFamily = FontFamily.Monospace,
                fontSize = scale.sp(0.95f),
                modifier = Modifier.padding(top = scale.dp(0.6f)),
            )
        }
        profile.title?.let {
            Text(
                text = it,
                color = accent,
                fontStyle = FontStyle.Italic,
                fontSize = scale.sp(1.05f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = scale.dp(0.6f)),
            )
        }
    }
}

@Composable
private fun Stats(profile: Profile, cursus: Cursus?, scale: CardScale) {
    val locale = LocalConfiguration.current.locales[0]
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(scale.dp(0.67f))) {
        StatBox(
            label = stringResource(R.string.label_level),
            value = cursus?.let { String.format(locale, "%.2f", it.level) } ?: "—",
            scale = scale,
            modifier = Modifier.weight(1f),
        )
        StatBox(stringResource(R.string.label_wallet), "${profile.wallet} ₳", scale, Modifier.weight(1f))
        StatBox(stringResource(R.string.label_points), profile.correctionPoints.toString(), scale, Modifier.weight(1f))
    }
}

@Composable
private fun StatBox(label: String, value: String, scale: CardScale, modifier: Modifier) {
    val shape = RoundedCornerShape(scale.dp(0.75f))
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, LineSoft, shape)
            .padding(vertical = scale.dp(0.6f), horizontal = scale.dp(0.35f)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label.uppercase(LocalConfiguration.current.locales[0]),
            color = Faint,
            fontSize = scale.sp(0.8f),
            letterSpacing = 0.08.em,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(value, fontFamily = FontFamily.Monospace, fontSize = scale.sp(1.17f), maxLines = 1)
    }
}

@Composable
private fun LocationTag(location: String?, scale: CardScale) {
    val color = if (location != null) Ok else Faint
    Row(
        modifier = Modifier
            .border(1.dp, if (location != null) Ok.copy(alpha = 0.45f) else Line, CircleShape)
            .padding(horizontal = scale.dp(0.9f), vertical = scale.dp(0.25f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(scale.dp(0.55f)),
    ) {
        Box(Modifier.size(scale.dp(0.55f)).background(color, CircleShape))
        Text(
            text = location ?: stringResource(R.string.card_offline),
            color = color,
            fontFamily = if (location != null) FontFamily.Monospace else FontFamily.Default,
            fontSize = scale.sp(0.92f),
        )
    }
}

private fun Profile.initials(): String =
    displayName.split(' ')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase(Locale.ROOT) }
        .ifEmpty { login.take(1).uppercase(Locale.ROOT) }

@Preview(widthDp = 360)
@Composable
private fun ProfileCardPreview() {
    SwiftyTheme {
        ProfileCard(previewProfile, previewProfile.mainCursus, Modifier.padding(16.dp))
    }
}
