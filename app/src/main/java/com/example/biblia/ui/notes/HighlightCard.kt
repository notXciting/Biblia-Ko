package com.example.biblia.ui.notes

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.HighlightColor
import com.example.biblia.ui.KeepAppLocale
import com.example.biblia.ui.reader.formatNoteDate

/**
 * One highlighted passage: colour bar, reference, date and the verse text.
 * Tap = go to it; the ⋮ menu changes the colour, adds a note, shares or removes.
 */
@Composable
internal fun HighlightCard(
    item: HighlightItem,
    onClick: () -> Unit,
    onChangeColor: (HighlightColor) -> Unit,
    onRemove: () -> Unit,
    onAddNote: () -> Unit,
    onShare: (Context) -> Unit,
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp)) {
            Box(
                Modifier
                    .width(5.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(item.passage.color.argb)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatNoteDate(item.passage.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Serif,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Outlined.MoreVert, stringResource(R.string.cd_more_options), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    KeepAppLocale {
                        Column {
                            ColorPickRow(current = item.passage.color) { onChangeColor(it); menu = false }
                            HorizontalDivider()
                            MenuItem(Icons.AutoMirrored.Outlined.MenuBook, stringResource(R.string.note_go_to_verse)) { menu = false; onClick() }
                            MenuItem(Icons.Outlined.EditNote, stringResource(R.string.highlight_add_note)) { menu = false; onAddNote() }
                            MenuItem(Icons.Outlined.Share, stringResource(R.string.action_share)) { menu = false; onShare(context) }
                            MenuItem(Icons.Outlined.FormatColorReset, stringResource(R.string.verse_action_remove_highlight)) { menu = false; onRemove() }
                        }
                    }
                }
            }
        }
    }
}

/** "Palitan ang kulay": the five highlight colours, current one ringed. */
@Composable
private fun ColorPickRow(current: HighlightColor, onPick: (HighlightColor) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.highlight_change_color),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HighlightColor.entries.forEach { color ->
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (color == current) MaterialTheme.colorScheme.onSurface else Color.Transparent)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color(color.argb))
                        .clickable { onPick(color) },
                )
            }
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label) }, leadingIcon = { Icon(icon, contentDescription = null) }, onClick = onClick)
}
