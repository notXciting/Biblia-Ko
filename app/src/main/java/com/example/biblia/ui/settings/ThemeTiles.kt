package com.example.biblia.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.AccentColor
import com.example.biblia.domain.model.ThemeMode
import com.example.biblia.ui.theme.ThemePalette
import com.example.biblia.ui.theme.ThemePalettes

private const val COLUMNS = 4

/**
 * Theme picker (System first). Each tile is a real preview: the theme's
 * background, its text color, and the current accent in the tone that theme
 * will actually use.
 */
@Composable
fun ThemeTiles(
    current: ThemeMode,
    accent: AccentColor,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ThemeMode.entries.chunked(COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { mode ->
                    ThemeTile(
                        label = stringResource(labelOf(mode)),
                        selected = current == mode,
                        onClick = { onSelect(mode) },
                        modifier = Modifier.weight(1f),
                    ) {
                        if (mode == ThemeMode.SYSTEM) {
                            Row(Modifier.fillMaxWidth().height(TILE_HEIGHT)) {
                                Preview(ThemePalettes.LIGHT, accent, Modifier.weight(1f))
                                Preview(ThemePalettes.DARK, accent, Modifier.weight(1f))
                            }
                        } else {
                            Preview(ThemePalettes.of(mode, systemDark = false), accent, Modifier.fillMaxWidth())
                        }
                    }
                }
                repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private val TILE_HEIGHT = 64.dp

@Composable
private fun ThemeTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TILE_HEIGHT)
                .clip(shape)
                .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = shape,
                )
                .clickable(onClick = onClick),
        ) { preview() }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** "Aa" in the theme's text color + a short accent bar, on the theme background. */
@Composable
private fun Preview(palette: ThemePalette, accent: AccentColor, modifier: Modifier) {
    Box(modifier.fillMaxHeight().background(Color(palette.background)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Aa",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = Color(palette.onBackground),
            )
            Box(
                Modifier
                    .padding(top = 3.dp)
                    .width(18.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(accent.tone(palette.dark))),
            )
        }
    }
}

private fun labelOf(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.SEPIA -> R.string.theme_sepia
    ThemeMode.PAPER -> R.string.theme_paper
    ThemeMode.SAGE -> R.string.theme_sage
    ThemeMode.DARK -> R.string.theme_dark
    ThemeMode.SLATE -> R.string.theme_slate
    ThemeMode.MIDNIGHT -> R.string.theme_midnight
}
