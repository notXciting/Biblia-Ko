package com.example.biblia.data.backup

import android.content.Context
import android.net.Uri
import com.example.biblia.BuildConfig
import com.example.biblia.data.AppIconManager
import com.example.biblia.data.repository.SettingsRepository
import com.example.biblia.data.user.HighlightEntity
import com.example.biblia.data.user.NoteEntity
import com.example.biblia.data.user.UserDatabase
import com.example.biblia.domain.backup.BackupCodec
import com.example.biblia.domain.backup.BackupData
import com.example.biblia.domain.backup.BackupMerge
import com.example.biblia.domain.backup.HighlightBackup
import com.example.biblia.domain.backup.ImportResult
import com.example.biblia.domain.backup.NoteBackup
import com.example.biblia.domain.model.NoteVerses
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Export / import of the user's own data (settings, highlights, notes) as a
 * JSON file the user picks with the system file picker (no permissions).
 */
class BackupRepository(
    private val context: Context,
    private val userDatabase: UserDatabase,
    private val settings: SettingsRepository,
) {

    suspend fun export(uri: Uri): BackupData = withContext(Dispatchers.IO) {
        val data = BackupData(
            exportedAt = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME,
            settings = settings.initial(),
            highlights = currentHighlights(),
            notes = currentNotes(),
        )
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("Cannot write $uri")
        out.bufferedWriter(Charsets.UTF_8).use { it.write(BackupCodec.encode(data)) }
        data
    }

    /** Reads and validates a backup file; throws BackupFormatException if it isn't one. */
    suspend fun read(uri: Uri): BackupData = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: error("Cannot read $uri")
        val raw = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        BackupCodec.decode(raw)
    }

    /** Merges [data] into this phone (see [BackupMerge]); nothing is deleted. */
    suspend fun apply(data: BackupData): ImportResult = withContext(Dispatchers.IO) {
        val highlights = BackupMerge.highlightsToWrite(currentHighlights(), data.highlights)
        val notes = BackupMerge.notesToAdd(currentNotes(), data.notes)
        userDatabase.highlightDao().upsert(
            highlights.map { HighlightEntity(it.book, it.chapter, it.verse, it.color, it.updatedAt) }
        )
        val noteDao = userDatabase.noteDao()
        notes.forEach { n ->
            noteDao.upsert(
                NoteEntity(
                    book = n.book, chapter = n.chapter, firstVerse = n.verses.min(),
                    verses = NoteVerses.encode(n.verses), text = n.text,
                    createdAt = n.createdAt, updatedAt = n.updatedAt,
                )
            )
        }
        data.settings?.let { restored ->
            settings.restorePreferences(restored)
            AppIconManager.apply(context, restored.appIcon)
        }
        ImportResult(highlights.size, notes.size, data.settings != null)
    }

    private suspend fun currentHighlights(): List<HighlightBackup> =
        userDatabase.highlightDao().all().map { HighlightBackup(it.book, it.chapter, it.verse, it.color, it.updatedAt) }

    private suspend fun currentNotes(): List<NoteBackup> =
        userDatabase.noteDao().all().mapNotNull { n ->
            val verses = NoteVerses.decode(n.verses)
            if (verses.isEmpty()) null else NoteBackup(n.book, n.chapter, verses, n.text, n.createdAt, n.updatedAt)
        }
}
