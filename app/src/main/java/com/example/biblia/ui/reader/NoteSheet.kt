@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.biblia.ui.reader

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.ui.KeepAppLocale
import java.text.DateFormat
import java.util.Date
import com.example.biblia.ui.theme.LocalReadingType

/**
 * Note editor for a verse selection: reference, a short quote of the verses,
 * and the note. Saving blank text removes an existing note.
 */
@Composable
fun NoteSheet(
    state: NoteEditorState,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    /** Back / tap outside / swipe down: keeps the typed text. */
    onDismiss: (String) -> Unit,
    /** "Kanselahin": undo this session's changes. */
    onCancel: () -> Unit,
    /** Called ~0.8 s after typing stops. */
    onAutosave: (String) -> Unit,
    /** Shown from the notes list: jump to the verse (gets the typed text so edits are kept). */
    onGoToVerse: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    var text by rememberSaveable(state.noteId, state.verses) { mutableStateOf(state.text) }
    val focus = remember { FocusRequester() }
    ModalBottomSheet(
        onDismissRequest = { onDismiss(text) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        KeepAppLocale {
            Column(
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    text = stringResource(if (state.noteId == null) R.string.note_new_title else R.string.note_edit_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = state.reference,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                state.updatedAt?.let { time ->
                    Text(
                        text = stringResource(R.string.note_last_edited, formatNoteDate(time)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.quote.isNotBlank()) {
                    Text(
                        text = "“" + state.quote + "”",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = LocalReadingType.current.family,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    if (onGoToVerse != null) {
                        AssistChip(
                            onClick = { onGoToVerse(text) },
                            label = { Text(stringResource(R.string.note_go_to_verse)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, null, Modifier.size(18.dp)) },
                        )
                    }
                    AssistChip(
                        onClick = { shareNote(context, state, text) },
                        enabled = text.isNotBlank(),
                        label = { Text(stringResource(R.string.action_share)) },
                        leadingIcon = { Icon(Icons.Outlined.Share, null, Modifier.size(18.dp)) },
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.note_hint)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = LocalReadingType.current.family),
                    minLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .padding(top = 8.dp)
                        .focusRequester(focus),
                )
                Text(
                    text = stringResource(R.string.note_autosave_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    if (state.noteId != null) {
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.note_delete))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
                    Button(
                        onClick = { onSave(text) },
                        enabled = text.isNotBlank() || state.noteId != null,
                    ) { Text(stringResource(R.string.note_save)) }
                }
            }
        }
    }
    // New note: open the keyboard right away.
    LaunchedEffect(state.noteId) { if (state.noteId == null) runCatching { focus.requestFocus() } }
    // Autosave: write what was typed shortly after typing stops.
    LaunchedEffect(text) {
        delay(AUTOSAVE_DELAY_MS)
        onAutosave(text)
    }
}

/** "Juan 3:16" + the verse text + the note, through the system share sheet. */
private fun shareNote(context: Context, state: NoteEditorState, text: String) {
    val body = buildString {
        append(state.reference)
        if (state.quote.isNotBlank()) append('\n').append('“').append(state.quote).append('”')
        append("\n\n").append(text.trim())
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(send, null))
}

/** Medium date in the app's UI language ("Nob 24, 2026" / "Nov 24, 2026"). */
@Composable
internal fun formatNoteDate(time: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(time, locale) { DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(time)) }
}

private const val AUTOSAVE_DELAY_MS = 800L
