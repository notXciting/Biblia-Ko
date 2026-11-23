package com.example.biblia

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.biblia.domain.model.NoteVerses
import com.example.biblia.domain.model.UiScale
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseLayout
import com.example.biblia.domain.model.VerseNote
import com.example.biblia.domain.model.VerseSpans
import com.example.biblia.ui.reader.buildChapterFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesAndScaleTest {

    private fun note(id: Long, vararg verses: Int) = VerseNote(id, 43, 3, verses.toSet(), "note $id", 0)

    @Test
    fun verseSetsRoundTripSortedAndIgnoreJunk() {
        assertEquals("16,17,18", NoteVerses.encode(listOf(18, 16, 17, 16)))
        assertEquals(setOf(16, 17, 18), NoteVerses.decode("16,17,18"))
        assertEquals(setOf(3, 5), NoteVerses.decode(" 5, x,3,,0,-1"))
        assertTrue(NoteVerses.decode("").isEmpty())
    }

    @Test
    fun selectingTheSameVersesFindsTheSameNote() {
        val notes = listOf(note(1, 16), note(2, 16, 17))
        assertEquals(2L, NoteVerses.exact(notes, setOf(17, 16))?.id)
        assertEquals(1L, NoteVerses.exact(notes, setOf(16))?.id)
        assertNull(NoteVerses.exact(notes, setOf(17)))
    }

    @Test
    fun markerSitsAfterTheLastVerseAndOnCombinedVerses() {
        val notes = listOf(note(1, 16, 17), note(2, 17), note(3, 2))
        // Verse 1 spans 1-2 (combined): a note on 2 is marked on 1.
        val markers = NoteVerses.markers(notes, mapOf(1 to 2))
        assertEquals(mapOf(17 to listOf(1L, 2L), 1 to listOf(3L)), markers)
        assertEquals(1, VerseSpans.ownerOf(2, mapOf(1 to 2)))
    }

    @Test
    fun readerShowsANoteIconOutsideTheVerseRange() {
        val verses = listOf(Verse(43, 3, 16, "For God so loved"), Verse(43, 3, 17, "For God sent not"))
        for (layout in VerseLayout.entries) {
            val flow = buildChapterFlow(verses, Color.Blue, 18.sp, layout, noted = setOf(16))
            assertEquals(1, flow.markers.size)
            val (range, verse) = flow.markers.single()
            assertEquals(16, verse)
            // Tapping the icon must not count as tapping a verse.
            assertTrue(flow.ranges.none { r -> range.first in r })
            assertTrue(range.first > flow.ranges[0].last && range.last < flow.ranges[1].first)
            // The verse text itself is unchanged.
            assertTrue(flow.annotated.text.substring(flow.ranges[0].first, flow.ranges[0].last + 1).endsWith("For God so loved"))
        }
    }

    @Test
    fun uiScaleClampsAndSnapsToFivePercent() {
        assertEquals(1f, UiScale.clamp(1f))
        assertEquals(1.1f, UiScale.clamp(1.1123f))
        assertEquals(0.85f, UiScale.clamp(0.8499f))
        assertEquals(UiScale.MIN, UiScale.clamp(0.1f))
        assertEquals(UiScale.MAX, UiScale.clamp(9f))
    }
}
