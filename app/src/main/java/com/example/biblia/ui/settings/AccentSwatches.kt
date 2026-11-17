package com.example.biblia.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.AccentColor

private const val PER_ROW = 5

/**
 * Accent picker: 10 swatches in two rows, each drawn in the tone the CURRENT
 * theme will actually use (deep on light themes, light on dark themes).
 */
@Composable
fun AccentSwatches(
    current: AccentColor,
    darkTheme: Boolean,
    onSelect: (AccentColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AccentColor.entries.chunked(PER_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { accent ->
                    Swatch(accent, Color(accent.tone(darkTheme)), accent == current, { onSelect(accent) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Swatch(accent: AccentColor, color: Color, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val label = stringResource(accentLabel(accent))
    val ring by animateDpAsState(if (selected) 2.5.dp else 0.dp, label = "accentRing")
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .aspectRatio(1f)
                .semantics { contentDescription = label }
                .border(ring, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f), CircleShape)
                .padding(5.dp)
                .clip(CircleShape)
                .background(color)
                .clickable(onClick = onClick),
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun accentLabel(accent: AccentColor): Int = when (accent) {
    AccentColor.BLUE -> R.string.cd_accent_blue
    AccentColor.INDIGO -> R.string.cd_accent_indigo
    AccentColor.PURPLE -> R.string.cd_accent_purple
    AccentColor.PINK -> R.string.cd_accent_pink
    AccentColor.RED -> R.string.cd_accent_red
    AccentColor.AMBER -> R.string.cd_accent_amber
    AccentColor.BROWN -> R.string.cd_accent_brown
    AccentColor.GREEN -> R.string.cd_accent_green
    AccentColor.TEAL -> R.string.cd_accent_teal
    AccentColor.GRAPHITE -> R.string.cd_accent_graphite
}
