package com.example.biblia.ui.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.biblia.domain.model.Verse

/** Shared verse-level views: compact list row + grid tile; empty = dim/disabled. */

@Composable
internal fun VerseRow(verse: Verse, isCurrent: Boolean, onClick: () -> Unit) {
    val enabled = !verse.isEmpty
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrent) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                }
            )
            .alpha(if (enabled) 1f else 0.35f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = verse.verse.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(32.dp),
        )
        if (verse.text.isNotBlank()) {
            Text(
                text = verse.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun VerseTile(verse: Verse, isCurrent: Boolean, onClick: () -> Unit) {
    val enabled = !verse.isEmpty
    Box(
        modifier = Modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrent) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = verse.verse.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = if (isCurrent) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
