package com.example.biblia.ui.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.BarSize
import com.example.biblia.ui.components.fittingLabel

/**
 * Flat top bar: settings (left), tappable location title (center, crossfades
 * when the location changes), outlined version chip (right), hairline below.
 * Listen and "Aking Pag-aaral" live in [ReaderBottomBar], so the title keeps
 * the width. When even that is too narrow (long book names, large app or
 * display size) the title switches to the short book name ("1 Tes 4:16").
 */
@Composable
fun ReaderTopBar(
    locationLabel: String,
    shortLocationLabel: String,
    versionShortName: String,
    onSettingsClick: () -> Unit,
    onLocationClick: () -> Unit,
    onVersionClick: () -> Unit,
    modifier: Modifier = Modifier,
    barSize: BarSize = BarSize.NORMAL,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barSize.topHeightDp.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    onClick = onLocationClick,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.weight(1f),
                ) {
                    LocationTitle(locationLabel, shortLocationLabel, barSize)
                }
                Surface(
                    onClick = onVersionClick,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    Text(
                        text = versionShortName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        }
    }
}

/** "Juan 3:16 ⌄", or the short form when the full one doesn't fit the space it has. */
@Composable
private fun LocationTitle(full: String, short: String, barSize: BarSize) {
    val base = when (barSize) {
        BarSize.COMPACT -> MaterialTheme.typography.titleSmall
        BarSize.NORMAL -> MaterialTheme.typography.titleMedium
        BarSize.LARGE -> MaterialTheme.typography.titleLarge
    }
    val style = base.copy(fontWeight = FontWeight.SemiBold)
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        // Room for the text = this box minus the chevron (22dp).
        val roomPx = with(density) { (maxWidth - 22.dp).roundToPx() }.coerceAtLeast(0)
        val label = fittingLabel(full, short, style, roomPx)
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AnimatedContent(
                targetState = label,
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                label = "location",
                modifier = Modifier.weight(1f, fill = false),
            ) { text ->
                Text(text = text, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.cd_location_pill),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
