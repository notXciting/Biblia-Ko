package com.example.biblia.ui.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.biblia.data.BibleVersion
import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.di.AppContainer
import com.example.biblia.domain.model.HighlightColor
import com.example.biblia.domain.model.NoteVerses
import com.example.biblia.domain.model.VerseNote
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseSpans
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** One version's text for the compare sheet; [verses] null = not installed. */
data class CompareRow(
    val version: BibleVersion,
    val verses: List<Verse>?,
    /** Combined verses in this version's chapter (verse labels like "1-2"). */
    val spanEnds: Map<Int, Int> = emptyMap(),
)

/**
 * Note editor: the reference ("Juan 3:16-17"), the verse set it is saved
 * under, a quote of the verses, and the note being edited ([noteId] null = new).
 */
data class NoteEditorState(
    val reference: String,
    val book: Int,
    val chapter: Int,
    val verses: Set<Int>,
    val quote: String,
    val noteId: Long?,
    val text: String,
)

/** Compare sheet state: the reference plus one row per registered version. */
data class CompareState(val reference: String, val rows: List<CompareRow>)

/**
 * Actions on a verse selection that go beyond the reader's own state:
 * highlights and notes (user database), and comparing across versions.
 * Owned by [ReaderViewModel], running on its scope.
 */
class VerseActions(private val container: AppContainer, private val scope: CoroutineScope) {

    private val highlightRepo = container.highlightRepository
    private val noteRepo = container.noteRepository
    private var compareJob: Job? = null

    var compare by mutableStateOf<CompareState?>(null); private set
    var noteEditor by mutableStateOf<NoteEditorState?>(null); private set

    fun notes(book: Int, chapter: Int): Flow<List<VerseNote>> = noteRepo.observeChapter(book, chapter)

    /** Opens the editor for exactly [verses]: their existing note, or a new one. */
    fun openNote(reference: String, book: Int, chapter: Int, verses: Set<Int>, quote: String) {
        if (verses.isEmpty()) return
        scope.launch {
            val existing = NoteVerses.exact(noteRepo.chapter(book, chapter), verses)
            noteEditor = NoteEditorState(reference, book, chapter, verses, quote, existing?.id, existing?.text.orEmpty())
        }
    }

    /** Opens an existing note (marker tap in the reader). */
    fun openNoteById(id: Long, reference: (VerseNote) -> String, quote: (VerseNote) -> String) {
        scope.launch {
            val note = noteRepo.byId(id) ?: return@launch
            noteEditor = NoteEditorState(reference(note), note.book, note.chapter, note.verses, quote(note), note.id, note.text)
        }
    }

    /** Saves the editor's note (blank text deletes it) and closes the editor. */
    fun saveNote(text: String) {
        val editor = noteEditor ?: return
        noteEditor = null
        scope.launch { noteRepo.save(editor.noteId, editor.book, editor.chapter, editor.verses, text) }
    }

    fun deleteNote() {
        val id = noteEditor?.noteId
        noteEditor = null
        if (id != null) scope.launch { noteRepo.delete(id) }
    }

    fun closeNote() { noteEditor = null }

    fun highlights(book: Int, chapter: Int): Flow<Map<Int, HighlightColor>> =
        highlightRepo.observeChapter(book, chapter)

    fun highlight(book: Int, chapter: Int, verses: Set<Int>, color: HighlightColor) {
        scope.launch { highlightRepo.set(book, chapter, verses, color) }
    }

    fun clearHighlight(book: Int, chapter: Int, verses: Set<Int>) {
        scope.launch { highlightRepo.clear(book, chapter, verses) }
    }

    /** Loads the selected verses from every installed version (one query each). */
    fun openCompare(reference: String, book: Int, chapter: Int, verses: Set<Int>) {
        compareJob?.cancel()
        compareJob = scope.launch {
            val rows = BibleVersionRegistry.all.map { version ->
                val content = if (container.settingsRepository.isImported(version.id)) {
                    runCatching { container.bibleRepository.getChapter(version.id, book, chapter) }.getOrNull()
                } else {
                    null
                }
                val ends = content?.spanEnds.orEmpty()
                // A combined verse is included when ANY number it covers is selected.
                val text = content?.verses?.filter { v ->
                    !v.isEmpty && (v.verse..VerseSpans.end(v.verse, ends)).any { it in verses }
                }
                CompareRow(version, if (content == null) null else text, ends)
            }
            compare = CompareState(reference, rows)
        }
    }

    fun closeCompare() {
        compareJob?.cancel()
        compare = null
    }
}
