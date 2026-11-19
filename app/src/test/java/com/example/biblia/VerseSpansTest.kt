package com.example.biblia

import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseSpans
import com.example.biblia.domain.reference.ReferenceFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

/** Combined verses (MBB12/ASD put the text of 1-2 on verse 1 and leave 2 empty). */
class VerseSpansTest {

    private fun chapter(vararg texts: String) = texts.mapIndexed { i, t -> Verse(8, 1, i + 1, t) }

    // Ruth 1 shape: 1-2 combined, 3, 4, then 5-7 combined at the chapter end.
    private val ruth = chapter("Nang ang Israel...", "", "Namatay...", "ay nakapag-asawa...", "namatay rin...", "", "")
    private val ends = VerseSpans.ends(ruth)

    @Test
    fun spansRunThroughConsecutiveEmptyVersesIncludingChapterEnd() {
        assertEquals(mapOf(1 to 2, 5 to 7), ends)
        assertEquals("1-2", VerseSpans.label(1, ends))
        assertEquals("3", VerseSpans.label(3, ends))
        assertEquals("5-7", VerseSpans.label(5, ends))
    }

    @Test
    fun versionsWithoutEmptyVersesHaveNoSpans() {
        assertEquals(emptyMap<Int, Int>(), VerseSpans.ends(chapter("a", "b", "c")))
        // Leading empty verses have no owner and are ignored.
        assertEquals(mapOf(2 to 3), VerseSpans.ends(chapter("", "b", "")))
    }

    @Test
    fun mergedNumbersMapToTheirCombinedVerse() {
        assertEquals(1, VerseSpans.ownerOf(2, ends))
        assertEquals(5, VerseSpans.ownerOf(7, ends))
        assertEquals(3, VerseSpans.ownerOf(3, ends))
        assertEquals(1, VerseSpans.ownerOf(1, ends))
    }

    @Test
    fun selectionReferenceCoversMergedNumbers() {
        val covered = VerseSpans.covered(setOf(1, 3), ends)
        assertEquals(setOf(1, 2, 3), covered)
        assertEquals("Ruth 1:1-3", ReferenceFormatter.format("Ruth", 1, covered))
        assertEquals("Ruth 1:1-2", ReferenceFormatter.format("Ruth", 1, VerseSpans.covered(setOf(1), ends)))
    }
}
