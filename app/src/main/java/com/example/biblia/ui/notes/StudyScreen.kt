@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.biblia.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.biblia.R
import com.example.biblia.di.AppContainer
import com.example.biblia.domain.model.AppSettings
import com.example.biblia.domain.model.NoteSort
import com.example.biblia.ui.reader.NoteSheet

/**
 * "Aking Pag-aaral" (My Study): the user's notes and highlights in two tabs,
 * with one search box and one sort order for both. Opened from the reader's
 * top bar; jumping to a verse pops back to the reader ([onOpenReader]).
 */
@Composable
fun StudyScreen(container: AppContainer, settings: AppSettings, onBack: () -> Unit, onOpenReader: () -> Unit) {
    val notesVm: NotesViewModel = viewModel(factory = viewModelFactory { initializer { NotesViewModel(container) } })
    val highlightsVm: HighlightsViewModel = viewModel(factory = viewModelFactory { initializer { HighlightsViewModel(container) } })
    val notes by notesVm.notes.collectAsStateWithLifecycle()
    val highlights by highlightsVm.items.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabHasItems = if (tab == 0) !notes.isNullOrEmpty() else !highlights.isNullOrEmpty()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
            Text(stringResource(R.string.study_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(tabLabel(R.string.study_tab_notes, notes?.size)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(tabLabel(R.string.study_tab_highlights, highlights?.size)) })
        }
        if (tabHasItems) {
            OutlinedTextField(
                value = notesVm.query,
                onValueChange = notesVm::updateQuery,
                placeholder = { Text(stringResource(R.string.study_search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (notesVm.query.isNotEmpty()) {
                        IconButton(onClick = { notesVm.updateQuery("") }) { Icon(Icons.Outlined.Close, stringResource(R.string.cd_clear_search)) }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                FilterChip(
                    selected = settings.noteSort == NoteSort.RECENT,
                    onClick = { notesVm.setSort(NoteSort.RECENT) },
                    label = { Text(stringResource(R.string.notes_sort_recent)) },
                )
                FilterChip(
                    selected = settings.noteSort == NoteSort.BIBLE,
                    onClick = { notesVm.setSort(NoteSort.BIBLE) },
                    label = { Text(stringResource(R.string.notes_sort_bible)) },
                )
            }
        }
        val body = Modifier.weight(1f)
        if (tab == 0) {
            NotesTab(notesVm, notes, settings, body)
        } else {
            HighlightsTab(
                vm = highlightsVm,
                all = highlights,
                query = notesVm.query,
                settings = settings,
                onGoTo = { highlightsVm.goTo(it, onOpenReader) },
                onAddNote = { item ->
                    val verses = (item.shown.verse..item.shown.endVerse).toSet()
                    notesVm.openFor(item.shown.book, item.shown.chapter, verses, item.label, item.text)
                },
                modifier = body,
            )
        }
    }
    notesVm.editor?.let { editor ->
        NoteSheet(
            state = editor,
            onSave = notesVm.noteEditor::save,
            onDelete = notesVm.noteEditor::delete,
            onDismiss = notesVm.noteEditor::dismiss,
            onCancel = notesVm.noteEditor::cancel,
            onAutosave = notesVm.noteEditor::autosave,
            onGoToVerse = { text -> notesVm.goToVerse(text, onOpenReader) },
        )
    }
}

/** "Mga Tala (12)"; just the name until the list has loaded. */
@Composable
private fun tabLabel(res: Int, count: Int?): String =
    if (count == null) stringResource(res, 0).substringBefore(" (") else stringResource(res, count)
