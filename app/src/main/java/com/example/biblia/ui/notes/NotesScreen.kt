package com.example.biblia.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
 * "Mga Tala": all notes with search and sort. Tapping one opens it in the
 * note sheet, which can jump to the verse ([onOpenReader] pops back).
 */
@Composable
fun NotesScreen(
    container: AppContainer,
    settings: AppSettings,
    onBack: () -> Unit,
    onOpenReader: () -> Unit,
) {
    val vm: NotesViewModel = viewModel(factory = viewModelFactory { initializer { NotesViewModel(container) } })
    val all by vm.notes.collectAsStateWithLifecycle()
    // Book names follow the selected Bible's language: recompute when it changes.
    val visible = remember(all, vm.query, settings.noteSort, settings.versionId) {
        all?.let { vm.visible(it, settings.noteSort) }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
            }
            Text(
                text = stringResource(R.string.notes_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            all?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    text = stringResource(R.string.notes_count, it.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 16.dp),
                )
            }
        }
        if (!all.isNullOrEmpty()) {
            OutlinedTextField(
                value = vm.query,
                onValueChange = vm::updateQuery,
                placeholder = { Text(stringResource(R.string.notes_search_hint)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (vm.query.isNotEmpty()) {
                        IconButton(onClick = { vm.updateQuery("") }) {
                            Icon(Icons.Outlined.Close, stringResource(R.string.cd_clear_search))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                FilterChip(
                    selected = settings.noteSort == NoteSort.RECENT,
                    onClick = { vm.setSort(NoteSort.RECENT) },
                    label = { Text(stringResource(R.string.notes_sort_recent)) },
                )
                FilterChip(
                    selected = settings.noteSort == NoteSort.BIBLE,
                    onClick = { vm.setSort(NoteSort.BIBLE) },
                    label = { Text(stringResource(R.string.notes_sort_bible)) },
                )
            }
        }
        when {
            visible == null -> Unit // first load: nothing rather than a flash of "no notes"
            all.isNullOrEmpty() -> NotesEmptyState(searching = false, modifier = Modifier.weight(1f))
            visible.isEmpty() -> NotesEmptyState(searching = true, modifier = Modifier.weight(1f))
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f).navigationBarsPadding(),
            ) {
                items(visible, key = { it.id }) { note ->
                    NoteCard(note = note, reference = vm.reference(note), onClick = { vm.open(note) })
                }
            }
        }
    }
    vm.editor?.let { editor ->
        NoteSheet(
            state = editor,
            onSave = vm::save,
            onDelete = vm::delete,
            onDismiss = vm::close,
            onGoToVerse = { text -> vm.goToVerse(text, onOpenReader) },
        )
    }
}
