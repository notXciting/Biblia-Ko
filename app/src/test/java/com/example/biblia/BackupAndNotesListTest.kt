package com.example.biblia

import com.example.biblia.domain.backup.BackupCodec
import com.example.biblia.domain.backup.BackupData
import com.example.biblia.domain.backup.BackupFormatException
import com.example.biblia.domain.backup.BackupMerge
import com.example.biblia.domain.backup.HighlightBackup
import com.example.biblia.domain.backup.NoteBackup
import com.example.biblia.domain.model.AccentColor
import com.example.biblia.domain.model.AppSettings
import com.example.biblia.domain.model.NoteSort
import com.example.biblia.domain.model.NoteVerses
import com.example.biblia.domain.model.ThemeMode
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseNote
import com.example.biblia.domain.tts.TtsPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupAndNotesListTest {

    private val settings = AppSettings(
        themeMode = ThemeMode.MIDNIGHT, accent = AccentColor.TEAL, fontSizeSp = 22f, uiScale = 1.15f,
        noteSort = NoteSort.BIBLE, tts = TtsPrefs(rate = 1.25f, voices = mapOf("English" to "en-us-x-iol-local")),
    )
    private val backup = BackupData(
        exportedAt = 1_764_000_000_000, appVersion = "2.4.0", settings = settings,
        highlights = listOf(HighlightBackup(43, 3, 16, "YELLOW", 100)),
        notes = listOf(NoteBackup(43, 3, setOf(16, 17), "Pag-ibig ng Diyos “ñ” \"quoted\"\nline 2", 50, 60)),
    )

    @Test
    fun backupRoundTripsEverything() {
        val decoded = BackupCodec.decode(BackupCodec.encode(backup))
        assertEquals(backup.highlights, decoded.highlights)
        assertEquals(backup.notes, decoded.notes)
        val s = decoded.settings!!
        assertEquals(ThemeMode.MIDNIGHT, s.themeMode)
        assertEquals(AccentColor.TEAL, s.accent)
        assertEquals(22f, s.fontSizeSp)
        assertEquals(1.15f, s.uiScale)
        assertEquals(NoteSort.BIBLE, s.noteSort)
        assertEquals(1.25f, s.tts.rate)
        assertEquals(mapOf("English" to "en-us-x-iol-local"), s.tts.voices)
        assertEquals("2.4.0", decoded.appVersion)
    }

    @Test
    fun foreignOrNewerFilesAreRejected() {
        for (raw in listOf("hello", "{}", """{"app":"other","format":1}""", """{"app":"biblia-ko","format":99}""")) {
            val failed = runCatching { BackupCodec.decode(raw) }.exceptionOrNull()
            assertTrue(raw, failed is BackupFormatException)
        }
    }

    @Test
    fun missingOrBadValuesFallBackSafely() {
        val raw = """{"app":"biblia-ko","format":1,
            "settings":{"themeMode":"NEON","fontSizeSp":99,"uiScale":5},
            "highlights":[{"book":99,"chapter":1,"verse":1,"color":"RED"},{"book":1,"chapter":1,"verse":1,"color":"RED"}],
            "notes":[{"book":1,"chapter":1,"verses":[],"text":"x"},{"book":1,"chapter":1,"verses":[2,1],"text":"  ok  "}]}"""
        val d = BackupCodec.decode(raw)
        assertEquals(AppSettings().themeMode, d.settings!!.themeMode) // unknown enum -> default
        assertEquals(32f, d.settings!!.fontSizeSp)
        assertEquals(1.3f, d.settings!!.uiScale)
        assertEquals(1, d.highlights.size) // book 99 dropped
        assertEquals(listOf(NoteBackup(1, 1, setOf(1, 2), "ok", 0, 0)), d.notes) // empty verses dropped, trimmed
        assertNull(BackupCodec.decode("""{"app":"biblia-ko","format":1}""").settings)
    }

    @Test
    fun importMergesWithoutDuplicatesOrDowngrades() {
        val have = listOf(HighlightBackup(1, 1, 1, "RED", 200), HighlightBackup(1, 1, 2, "RED", 100))
        val incoming = listOf(
            HighlightBackup(1, 1, 1, "BLUE", 150), // older than ours: keep ours
            HighlightBackup(1, 1, 2, "BLUE", 300), // newer: take it
            HighlightBackup(1, 1, 3, "GREEN", 1), // new verse
        )
        assertEquals(listOf(incoming[1], incoming[2]), BackupMerge.highlightsToWrite(have, incoming))

        val note = NoteBackup(1, 1, setOf(1), "Same", 1, 1)
        val notes = BackupMerge.notesToAdd(
            current = listOf(note),
            incoming = listOf(note.copy(text = " Same ", updatedAt = 9), note.copy(text = "New"), note.copy(text = "New")),
        )
        assertEquals(listOf(note.copy(text = "New")), notes) // existing + in-file duplicates skipped
    }

    private fun n(id: Long, book: Int, chapter: Int, verse: Int, text: String, updated: Long) =
        VerseNote(id, book, chapter, setOf(verse), text, updated)

    @Test
    fun notesListSearchesTextAndReferenceAndSorts() {
        val notes = listOf(n(1, 43, 3, 16, "Pag-ibig", 10), n(2, 1, 1, 1, "Paglikha", 30), n(3, 19, 23, 1, "Pastol", 20))
        val ref = { note: VerseNote -> listOf("Juan", "Genesis", "Mga Awit")[listOf(43, 1, 19).indexOf(note.book)] + " " + note.chapter }
        assertEquals(listOf(2L, 3L, 1L), NoteVerses.list(notes, "", NoteSort.RECENT, ref).map { it.id })
        assertEquals(listOf(2L, 3L, 1L), NoteVerses.list(notes, "", NoteSort.BIBLE, ref).map { it.id })
        assertEquals(listOf(1L), NoteVerses.list(notes, "PAG-ÍBIG", NoteSort.RECENT, ref).map { it.id })
        assertEquals(listOf(3L), NoteVerses.list(notes, "awit", NoteSort.RECENT, ref).map { it.id }) // by book name
        assertTrue(NoteVerses.list(notes, "walang ganito", NoteSort.RECENT, ref).isEmpty())
    }

    @Test
    fun noteQuoteUsesTheVersesTextIncludingCombinedVerses() {
        val chapter = listOf(Verse(8, 1, 1, "Combined text"), Verse(8, 1, 2, ""), Verse(8, 1, 3, "Third"))
        val ends = mapOf(1 to 2)
        assertEquals("Combined text", NoteVerses.quote(chapter, ends, setOf(2)))
        assertEquals("Combined text Third", NoteVerses.quote(chapter, ends, setOf(1, 3)))
    }
}
