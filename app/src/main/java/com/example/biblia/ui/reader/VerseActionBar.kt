package com.example.biblia.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.HighlightColor

/**
 * Verse selection panel: reference header, highlighter colors, and actions
 * (copy, share, compare across versions, note). Slides up from the bottom.
 * [activeColor] = the color every selected verse already has (checkmark);
 * [anyHighlighted] shows the "remove highlight" button.
 */
@Composable
fun VerseActionBar(
    selectedCount: Int,
    reference: String,
    activeColor: HighlightColor?,
    anyHighlighted: Boolean,
    onHighlight: (HighlightColor) -> Unit,
    onRemoveHighlight: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onCompare: () -> Unit,
    onNote: () -> Unit,
    onClose: () -> Unit,
    /** Shown under the actions (cross-references); empty by default. */
    footer: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = selectedCount > 0,
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
            Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = reference,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.reader_selected_count, selectedCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.cd_clear_selection))
                    }
                }
                Spacer(Modifier.height(12.dp))
                HighlightRow(activeColor, anyHighlighted, onHighlight, onRemoveHighlight)
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(top = 14.dp, bottom = 12.dp, end = 12.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp),
                ) {
                    val item = Modifier.weight(1f)
                    ActionItem(Icons.Outlined.ContentCopy, stringResource(R.string.action_copy), onCopy, item)
                    ActionItem(Icons.Outlined.Share, stringResource(R.string.action_share), onShare, item)
                    ActionItem(Icons.Outlined.Translate, stringResource(R.string.verse_action_compare), onCompare, item)
                    ActionItem(Icons.Outlined.EditNote, stringResource(R.string.verse_action_note), onNote, item)
                }
                footer()
            }
        }
    }
}

@Composable
private fun HighlightRow(
    active: HighlightColor?,
    anyHighlighted: Boolean,
    onHighlight: (HighlightColor) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HighlightColor.entries.forEach { color ->
            Swatch(color, selected = color == active, onClick = { onHighlight(color) })
        }
        AnimatedVisibility(visible = anyHighlighted, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline), CircleShape)
                    .clickable(onClick = onRemove),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FormatColorReset,
                    contentDescription = stringResource(R.string.verse_action_remove_highlight),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun Swatch(color: HighlightColor, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.12f else 1f, label = "swatch")
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(Color(color.argb))
            .clickable(onClick = onClick),
    ) {
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF3A3328), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ActionItem(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
