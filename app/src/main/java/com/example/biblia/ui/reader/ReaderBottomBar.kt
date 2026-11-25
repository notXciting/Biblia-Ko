package com.example.biblia.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.BarSize

/**
 * Reading toolbar at the bottom: previous chapter, "Aking Pag-aaral", Listen,
 * next chapter. It slides away while reading downward and comes back when
 * scrolling up or changing chapter ([rememberHideOnScroll]); the verse panel
 * and the Listen player take its place when they are open.
 */
@Composable
fun ReaderBottomBar(
    visible: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStudy: () -> Unit,
    onListen: () -> Unit,
    modifier: Modifier = Modifier,
    barSize: BarSize = BarSize.NORMAL,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.navigationBarsPadding()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(barSize.bottomHeightDp.dp),
                ) {
                    IconButton(onClick = onPrevious, enabled = canGoBack) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.action_previous_chapter))
                    }
                    BarItem(Icons.Outlined.CollectionsBookmark, stringResource(R.string.study_title), onStudy, barSize, Modifier.weight(1f))
                    BarItem(Icons.Outlined.Headphones, stringResource(R.string.listen_action), onListen, barSize, Modifier.weight(1f))
                    IconButton(onClick = onNext, enabled = canGoForward) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.action_next_chapter))
                    }
                }
            }
        }
    }
}

/** Icon with a small label under it (labels make the two actions discoverable). */
@Composable
private fun BarItem(icon: ImageVector, label: String, onClick: () -> Unit, barSize: BarSize, modifier: Modifier) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Compact: icon only (the label becomes its description). Large: bigger icon and label.
            val compact = barSize == BarSize.COMPACT
            Icon(
                imageVector = icon,
                contentDescription = if (compact) label else null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(if (barSize == BarSize.LARGE) 26.dp else 22.dp),
            )
            if (!compact) {
                Text(
                    text = label,
                    style = if (barSize == BarSize.LARGE) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Hides the bar while the chapter text scrolls toward its end and shows it
 * when scrolling back. Horizontal pager swipes don't count. Returns the
 * connection to put on the reader body plus the visibility state.
 */
@Composable
fun rememberHideOnScroll(): Pair<NestedScrollConnection, MutableState<Boolean>> = remember {
    val shown = mutableStateOf(true)
    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y < -SCROLL_SLOP) shown.value = false
            else if (available.y > SCROLL_SLOP) shown.value = true
            return Offset.Zero
        }
    }
    connection to shown
}

private const val SCROLL_SLOP = 6f
