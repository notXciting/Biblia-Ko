package com.example.biblia.ui.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.di.AppContainer
import com.example.biblia.di.openInReader
import com.example.biblia.domain.model.NoteSort
import com.example.biblia.domain.model.NoteVerses
import com.example.biblia.domain.model.VerseNote
import com.example.biblia.domain.reference.ReferenceFormatter
import com.example.biblia.ui.reader.NoteEditorState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * "Mga Tala": every note (search + sort), opening one in the note sheet, and
 * jumping to its verse in the reader.
 */
class NotesViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.noteRepository
    private val settings = container.settingsRepository

    /** null until the first database emission (no "no notes" flash). */
    val notes: StateFlow<List<VerseNote>?> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var query by mutableStateOf(""); private set
    var editor by mutableStateOf<NoteEditorState?>(null); private set
    private var jumping = false

    fun updateQuery(value: String) { query = value }

    fun setSort(value: NoteSort) { viewModelScope.launch { settings.setNoteSort(value) } }

    fun visible(all: List<VerseNote>, sort: NoteSort): List<VerseNote> = NoteVerses.list(all, query, sort, ::reference)

    /** "Juan 3:16-17" in the selected Bible's book-name language. */
    fun reference(note: VerseNote): String =
        ReferenceFormatter.format(BookCatalog.book(note.book).displayName, note.chapter, note.verses)

    /** Opens the note with a quote of its verses from the Bible currently in use. */
    fun open(note: VerseNote) {
        viewModelScope.launch {
            val versionId = settings.initial().versionId
            val content = runCatching { container.chapterCache.get(versionId, note.book, note.chapter) }.getOrNull()
            val quote = content?.let { NoteVerses.quote(it.verses, it.spanEnds, note.verses) }.orEmpty()
            editor = NoteEditorState(
                reference(note), note.book, note.chapter, note.verses, quote, note.id, note.text, note.updatedAt,
            )
        }
    }

    /** Opens the note for exactly [verses] (from a highlight): theirs if it exists, else a new one. */
    fun openFor(book: Int, chapter: Int, verses: Set<Int>, reference: String, quote: String) {
        viewModelScope.launch {
            val existing = NoteVerses.exact(repo.chapter(book, chapter), verses)
            editor = NoteEditorState(
                reference, book, chapter, verses, quote, existing?.id, existing?.text.orEmpty(), existing?.updatedAt,
            )
        }
    }

    fun save(text: String) {
        val e = editor ?: return
        editor = null
        viewModelScope.launch { repo.save(e.noteId, e.book, e.chapter, e.verses, text) }
    }

    fun delete() {
        val id = editor?.noteId
        editor = null
        if (id != null) viewModelScope.launch { repo.delete(id) }
    }

    fun close() { editor = null }

    /**
     * Opens the note's first verse in the reader, then [onReady] pops back to
     * it. Edits typed in the sheet ([text]) are saved first, never lost.
     */
    fun goToVerse(text: String, onReady: () -> Unit) {
        val e = editor ?: return
        if (jumping) return
        jumping = true
        editor = null
        viewModelScope.launch {
            if (text.trim() != e.text.trim() && text.isNotBlank()) repo.save(e.noteId, e.book, e.chapter, e.verses, text)
            container.openInReader(settings.initial().versionId, e.book, e.chapter, e.verses.min())
            onReady()
            jumping = false
        }
    }
}
