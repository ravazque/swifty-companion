package com.ravazque.swiftycompanion.ui.profile.card

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import com.ravazque.swiftycompanion.R
import com.ravazque.swiftycompanion.model.Cursus
import com.ravazque.swiftycompanion.ui.theme.Faint

@Composable
internal fun ColumnScope.CardBack(cursus: Cursus?, accent: Color, scale: CardScale) {
    val skills = cursus?.chartSkills.orEmpty()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.skills_title).uppercase(LocalConfiguration.current.locales[0]),
            color = accent,
            fontSize = scale.sp(0.95f),
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.08.em,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        cursus?.let { Tag(it.name, Faint, scale) }
    }
    val body = Modifier.weight(1f).fillMaxWidth().padding(vertical = scale.dp(0.8f))
    if (skills.isEmpty()) {
        Box(body, contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.skills_empty), color = Faint, fontSize = scale.sp(1.05f))
        }
    } else {
        SkillRadar(skills, accent, scale, body)
    }
    FlipHint(scale, Modifier.align(Alignment.End))
}
