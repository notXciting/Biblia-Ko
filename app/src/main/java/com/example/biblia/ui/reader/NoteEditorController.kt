package com.example.biblia.ui.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.biblia.data.user.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The open note sheet, with autosave (shared by the reader and "Aking
 * Pag-aaral"). What the user types is written shortly after they stop typing
 * ([autosave]), and kept when the sheet is closed by Back / tapping outside
 * ([dismiss]); nothing typed is lost. "Kanselahin" ([cancel]) undoes this
 * session: an existing note gets its original text back, a new one is removed.
 * Writes are serialised, so a new note is created once and then updated.
 */
class NoteEditorController(private val repo: NoteRepository, private val scope: CoroutineScope) {

    var state by mutableStateOf<NoteEditorState?>(null); private set

    private val mutex = Mutex()
    /** The note's row id; set by the first write of a new note. */
    private var noteId: Long? = null
    private var originalId: Long? = null
    private var originalText = ""
    private var savedText = ""

    fun show(editor: NoteEditorState) {
        state = editor
        noteId = editor.noteId
        originalId = editor.noteId
        originalText = editor.text.trim()
        savedText = originalText
    }

    /** Debounced by the sheet. Blank text is never autosaved (it would delete the note). */
    fun autosave(text: String) {
        if (text.isNotBlank()) write(text)
    }

    /** Back, tap outside, swipe down: keep what was typed. */
    fun dismiss(text: String) {
        autosave(text)
        state = null
    }

    /** "I-save": keep it (blank text removes the note, as before). */
    fun save(text: String) {
        val s = state ?: return
        state = null
        if (text.isNotBlank()) { write(text, s); return }
        scope.launch { mutex.withLock { noteId?.let { repo.delete(it) }; noteId = null } }
    }

    /** "Kanselahin": undo this editing session. */
    fun cancel() {
        val s = state ?: return
        state = null
        scope.launch {
            mutex.withLock {
                val id = noteId
                when {
                    originalId == null && id != null -> repo.delete(id) // a new note that was autosaved
                    originalId != null && savedText != originalText ->
                        repo.save(originalId, s.book, s.chapter, s.verses, originalText)
                }
            }
        }
    }

    fun delete() {
        state = null
        scope.launch { mutex.withLock { noteId?.let { repo.delete(it) }; noteId = null } }
    }

    private fun write(text: String, s: NoteEditorState? = state) {
        s ?: return
        val trimmed = text.trim()
        if (trimmed == savedText) return
        savedText = trimmed
        scope.launch {
            mutex.withLock { noteId = repo.save(noteId, s.book, s.chapter, s.verses, trimmed) ?: noteId }
        }
    }
}
