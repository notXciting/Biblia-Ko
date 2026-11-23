package com.example.biblia.data.user

import com.example.biblia.domain.model.NoteVerses
import com.example.biblia.domain.model.VerseNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Personal verse notes (user.db, version-independent). */
class NoteRepository(private val dao: NoteDao) {

    fun observeChapter(book: Int, chapter: Int): Flow<List<VerseNote>> =
        dao.observeChapter(book, chapter).map { rows -> rows.mapNotNull(::toNote) }

    suspend fun chapter(book: Int, chapter: Int): List<VerseNote> = dao.chapter(book, chapter).mapNotNull(::toNote)

    suspend fun byId(id: Long): VerseNote? = dao.byId(id)?.let(::toNote)

    /**
     * Creates ([id] null) or updates a note. Blank text deletes an existing
     * note instead (clearing the text is how a note is removed too).
     */
    suspend fun save(id: Long?, book: Int, chapter: Int, verses: Set<Int>, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            if (id != null) dao.delete(id)
            return
        }
        val now = System.currentTimeMillis()
        val existing = id?.let { dao.byId(it) }
        dao.upsert(
            NoteEntity(
                id = id ?: 0,
                book = book,
                chapter = chapter,
                firstVerse = verses.min(),
                verses = NoteVerses.encode(verses),
                text = trimmed,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
        )
    }

    suspend fun delete(id: Long) = dao.delete(id)

    private fun toNote(row: NoteEntity): VerseNote? {
        val verses = NoteVerses.decode(row.verses)
        if (verses.isEmpty()) return null
        return VerseNote(row.id, row.book, row.chapter, verses, row.text, row.updatedAt)
    }
}
