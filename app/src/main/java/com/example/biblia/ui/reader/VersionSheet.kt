@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.biblia.ui.reader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.data.BibleVersion
import com.example.biblia.data.importer.ImportState
import com.example.biblia.ui.KeepAppLocale

/**
 * Version switcher. Each row: abbreviation badge, full name + language, and a
 * check for the current version. Selecting an uninstalled version imports it
 * (progress drawn inside the row) and switches when done, keeping the verse.
 */
@Composable
fun VersionBottomSheet(
    versions: List<BibleVersion>,
    currentVersionId: String,
    importStates: Map<String, ImportState>,
    onSelect: (BibleVersion) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Fully expanded: a half-expanded sheet re-anchors (slides) whenever
        // its content height changes, which pushed the tapped row off-screen.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { KeepAppLocale { Column {
        Text(
            text = stringResource(R.string.versions_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        // Scrollable: 10 versions no longer fit on one screen.
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            versions.forEach { version ->
                VersionRow(
                    version = version,
                    selected = version.id == currentVersionId,
                    state = importStates[version.id] ?: ImportState(version.id),
                    onClick = { onSelect(version) },
                )
            }
        }
    } } }
}

@Composable
private fun VersionRow(
    version: BibleVersion,
    selected: Boolean,
    state: ImportState,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // Import progress is DRAWN (row fill + bottom line), never laid out, so the
    // row keeps its height while a version imports and nothing shifts.
    val progress by animateFloatAsState(
        targetValue = if (state.isRunning) state.percent / 100f else 0f,
        label = "importProgress",
    )
    val fillColor = colors.primary.copy(alpha = 0.12f)
    val lineColor = colors.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) colors.primaryContainer else colors.surfaceContainerLow)
            .drawBehind {
                if (state.isRunning) {
                    val w = size.width * progress
                    drawRect(fillColor, size = Size(w, size.height))
                    val line = 3.dp.toPx()
                    drawRect(lineColor, topLeft = Offset(0f, size.height - line), size = Size(w, line))
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 56.dp, height = 40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) colors.primary else colors.surfaceContainerHighest),
            ) {
                Text(
                    text = version.shortName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = version.fullName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                )
                Text(
                    text = if (state.isRunning) {
                        stringResource(R.string.version_preparing, state.percent)
                    } else {
                        version.language
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.isRunning) colors.primary else colors.onSurfaceVariant,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = colors.primary,
                )
            }
        }
    }
}
