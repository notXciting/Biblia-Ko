package com.example.biblia.ui.bibles

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.data.BibleVersion
import com.example.biblia.data.importer.ImportState

/** What a row offers, decided by the screen. */
internal enum class BibleRowAction { LOAD, PROGRESS, IN_USE, UNLOAD, UNLOADING }

/**
 * One Bible: badge, name, "~8 MB" (or "Inihahanda… 42%"), and its action.
 * Import progress is drawn behind the row (never laid out), like the version sheet.
 */
@Composable
internal fun BibleRow(
    version: BibleVersion,
    sizeMb: Int?,
    state: ImportState,
    action: BibleRowAction,
    canUnload: Boolean,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val progress by animateFloatAsState(if (state.isRunning) state.percent / 100f else 0f, label = "bibleImport")
    val fill = colors.primary.copy(alpha = 0.12f)
    val line = colors.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceContainerLow)
            .drawBehind {
                if (state.isRunning) {
                    val w = size.width * progress
                    drawRect(fill, size = Size(w, size.height))
                    drawRect(line, topLeft = Offset(0f, size.height - 3.dp.toPx()), size = Size(w, 3.dp.toPx()))
                }
            }
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 56.dp, height = 40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (action == BibleRowAction.IN_USE) colors.primary else colors.surfaceContainerHighest),
        ) {
            Text(
                text = version.shortName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (action == BibleRowAction.IN_USE) colors.onPrimary else colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = version.fullName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when {
                    state.isRunning -> stringResource(R.string.version_preparing, state.percent)
                    action == BibleRowAction.UNLOADING -> stringResource(R.string.bibles_unloading)
                    sizeMb != null -> stringResource(R.string.bibles_size, sizeMb)
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (state.isRunning) colors.primary else colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        when (action) {
            BibleRowAction.LOAD -> FilledTonalButton(onClick = onLoad) { Text(stringResource(R.string.bibles_load)) }
            BibleRowAction.UNLOAD -> OutlinedButton(onClick = onUnload, enabled = canUnload) {
                Text(stringResource(R.string.bibles_unload))
            }
            BibleRowAction.IN_USE -> AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.bibles_in_use)) })
            BibleRowAction.PROGRESS, BibleRowAction.UNLOADING -> Unit
        }
    }
}
