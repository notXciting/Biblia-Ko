package com.example.biblia.ui.listen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.BarSize
import com.example.biblia.ui.components.FittingText

/**
 * Floating "Listen" transport: the verse being read, previous/next verse,
 * play/pause and stop. Shown while reading aloud is playing or paused.
 */
@Composable
fun ListenBar(
    visible: Boolean,
    reference: String,
    /** Same reference with the short book name, used when [reference] doesn't fit. */
    shortReference: String,
    playing: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    barSize: BarSize = BarSize.NORMAL,
) {
    val vertical = when (barSize) { BarSize.COMPACT -> 2.dp; BarSize.NORMAL -> 8.dp; BarSize.LARGE -> 12.dp }
    val playSize = when (barSize) { BarSize.COMPACT -> 40.dp; BarSize.NORMAL -> 48.dp; BarSize.LARGE -> 56.dp }
    val referenceStyle = when (barSize) {
        BarSize.COMPACT -> MaterialTheme.typography.labelLarge
        BarSize.NORMAL -> MaterialTheme.typography.titleSmall
        BarSize.LARGE -> MaterialTheme.typography.titleMedium
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 20.dp, end = 6.dp, top = vertical, bottom = vertical),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(if (playing) R.string.listen_reading else R.string.listen_paused),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                    FittingText(
                        full = reference,
                        short = shortReference,
                        style = referenceStyle.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Rounded.SkipPrevious, stringResource(R.string.listen_previous_verse))
                }
                FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(playSize)) {
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(if (playing) R.string.listen_pause else R.string.listen_play),
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, stringResource(R.string.listen_next_verse))
                }
                Spacer(Modifier.width(2.dp))
                IconButton(onClick = onStop) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.listen_stop),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
