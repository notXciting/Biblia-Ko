package com.example.biblia.domain.backup

import com.example.biblia.domain.model.AppSettings

/** One highlight in a backup file (version-independent, like in user.db). */
data class HighlightBackup(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val color: String,
    val updatedAt: Long,
)

/** One note in a backup file. */
data class NoteBackup(
    val book: Int,
    val chapter: Int,
    val verses: Set<Int>,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Everything the user made, for moving to another phone: preferences,
 * highlights and notes. Not included: the Bible texts (bundled with the app),
 * the reading position and which Bible is open (device state).
 */
data class BackupData(
    val exportedAt: Long,
    val appVersion: String,
    val settings: AppSettings?,
    val highlights: List<HighlightBackup>,
    val notes: List<NoteBackup>,
)

/** What an import changed (shown to the user afterwards). */
data class ImportResult(val highlightsAdded: Int, val notesAdded: Int, val settingsApplied: Boolean)

/**
 * Import merges, it never deletes: highlights are added, or replaced only by a
 * NEWER copy of the same verse; a note is added unless the same note (same
 * verses and text) already exists, so importing a file twice is harmless.
 */
object BackupMerge {

    fun highlightsToWrite(current: List<HighlightBackup>, incoming: List<HighlightBackup>): List<HighlightBackup> {
        val existing = current.associateBy { Triple(it.book, it.chapter, it.verse) }
        return incoming.filter { h ->
            val have = existing[Triple(h.book, h.chapter, h.verse)]
            have == null || (h.updatedAt > have.updatedAt && h.color != have.color)
        }
    }

    fun notesToAdd(current: List<NoteBackup>, incoming: List<NoteBackup>): List<NoteBackup> {
        val seen = current.mapTo(HashSet()) { key(it) }
        return incoming.filter { seen.add(key(it)) } // also drops duplicates inside the file
    }

    private fun key(n: NoteBackup) = listOf(n.book, n.chapter, n.verses.sorted(), n.text.trim())
}
