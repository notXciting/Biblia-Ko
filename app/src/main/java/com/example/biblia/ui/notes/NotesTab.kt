package com.example.biblia.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.AppSettings
import com.example.biblia.domain.model.VerseNote

/** "Mga Tala": the notes list (search + sort come from the Study screen). */
@Composable
internal fun NotesTab(vm: NotesViewModel, all: List<VerseNote>?, settings: AppSettings, modifier: Modifier) {
    // Book names follow the selected Bible's language: recompute when it changes.
    val visible = remember(all, vm.query, settings.noteSort, settings.versionId) { all?.let { vm.visible(it, settings.noteSort) } }
    val listState = rememberLazyListState()
    LaunchedEffect(vm.query, settings.noteSort) { listState.scrollToItem(0) }
    when {
        visible == null -> Unit // first load: nothing rather than a flash of "no notes"
        all.isNullOrEmpty() -> StudyEmptyState(
            Icons.Outlined.EditNote, stringResource(R.string.notes_empty), stringResource(R.string.notes_empty_hint), modifier,
        )
        visible.isEmpty() -> StudyEmptyState(
            Icons.Outlined.SearchOff, stringResource(R.string.notes_none_found), stringResource(R.string.notes_none_found_hint), modifier,
        )
        else -> LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier.navigationBarsPadding(),
        ) {
            items(visible, key = { it.id }) { note ->
                NoteCard(note = note, reference = vm.reference(note), onClick = { vm.open(note) })
            }
        }
    }
}
