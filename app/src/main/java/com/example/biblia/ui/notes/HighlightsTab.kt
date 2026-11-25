package com.example.biblia.ui.notes

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.domain.model.AppSettings
import com.example.biblia.domain.model.HighlightColor

/** "Mga Marka": colour filter, then every highlighted passage (tap = go to it). */
@Composable
internal fun HighlightsTab(
    vm: HighlightsViewModel,
    all: List<HighlightItem>?,
    query: String,
    settings: AppSettings,
    onGoTo: (HighlightItem) -> Unit,
    onAddNote: (HighlightItem) -> Unit,
    modifier: Modifier,
) {
    val visible = remember(all, query, settings.noteSort, vm.colors) { all?.let { vm.visible(it, query, settings.noteSort) } }
    val listState = rememberLazyListState()
    LaunchedEffect(query, settings.noteSort, vm.colors) { listState.scrollToItem(0) }
    if (visible == null) return
    if (all.isNullOrEmpty()) {
        StudyEmptyState(
            Icons.Outlined.BorderColor, stringResource(R.string.highlights_empty), stringResource(R.string.highlights_empty_hint), modifier,
        )
        return
    }
    Column(modifier) {
        ColorFilterRow(selected = vm.colors, onToggle = vm::toggleColor)
        if (visible.isEmpty()) {
            StudyEmptyState(
                Icons.Outlined.SearchOff, stringResource(R.string.highlights_none_found), stringResource(R.string.notes_none_found_hint),
                Modifier.weight(1f),
            )
            return@Column
        }
        val shortName = BibleVersionRegistry.byId(settings.versionId).shortName
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).navigationBarsPadding(),
        ) {
            items(visible, key = { "${it.passage.book}:${it.passage.chapter}:${it.passage.startVerse}:${it.passage.color}" }) { item ->
                HighlightCard(
                    item = item,
                    onClick = { onGoTo(item) },
                    onChangeColor = { vm.changeColor(item, it) },
                    onRemove = { vm.remove(item) },
                    onAddNote = { onAddNote(item) },
                    onShare = { context -> shareHighlight(context, item, shortName) },
                )
            }
        }
    }
}

/** Five colour dots; none selected = all colours. */
@Composable
private fun ColorFilterRow(selected: Set<HighlightColor>, onToggle: (HighlightColor) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp),
    ) {
        HighlightColor.entries.forEach { color ->
            val on = color in selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(color.argb))
                    .then(if (on) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                    .clickable { onToggle(color) },
            ) {
                if (on) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF3A3328), modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(4.dp))
    }
}

/** “text” - Juan 3:16-17 (MBB12), through the system share sheet (same form as the verse panel). */
private fun shareHighlight(context: Context, item: HighlightItem, shortName: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "“" + item.text + "” - " + item.label + " (" + shortName + ")")
    }
    context.startActivity(Intent.createChooser(send, null))
}
