package com.example.biblia

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseLayout
import com.example.biblia.ui.reader.buildChapterFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Both layouts must map every verse to exactly "<number><sep><text>". */
class VerseLayoutTest {

    private val verses = listOf(
        Verse(19, 23, 1, "The LORD is my shepherd; I shall not want."),
        Verse(19, 23, 2, ""), // empty verses are skipped in both layouts
        Verse(19, 23, 3, "He restoreth my soul."),
        Verse(19, 23, 4, "Yea, though I walk through the valley."),
    )

    private fun check(layout: VerseLayout) {
        val flow = buildChapterFlow(verses, Color.Blue, 18.sp, layout)
        val text = flow.annotated.text
        assertEquals(listOf(1, 3, 4), flow.numbers)
        flow.ranges.forEachIndexed { i, range ->
            val verse = verses.first { it.verse == flow.numbers[i] }
            val slice = text.substring(range.first, range.last + 1)
            assertTrue("$layout: '$slice'", slice.startsWith(verse.verse.toString()))
            assertTrue("$layout: '$slice'", slice.endsWith(verse.text))
        }
        // Ranges are ordered and never overlap (tap / highlight correctness).
        flow.ranges.zipWithNext().forEach { (a, b) -> assertTrue(a.last < b.first) }
    }

    @Test fun paragraphRanges() = check(VerseLayout.PARAGRAPH)

    @Test fun verseByVerseRanges() = check(VerseLayout.VERSE_BY_VERSE)

    @Test
    fun verseByVerseGapsAreOutsideVerses() {
        val flow = buildChapterFlow(verses, Color.Blue, 18.sp, VerseLayout.VERSE_BY_VERSE)
        val text = flow.annotated.text
        val gaps = text.indices.filter { text[it] == '​' }
        assertEquals(2, gaps.size) // between 3 visible verses
        gaps.forEach { g -> assertTrue(flow.ranges.none { g in it }) }
        // One paragraph per verse + one per gap.
        assertEquals(5, flow.annotated.paragraphStyles.size)
    }
}
