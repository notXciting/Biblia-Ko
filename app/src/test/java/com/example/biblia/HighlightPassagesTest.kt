package com.example.biblia

import com.example.biblia.domain.crossref.CrossRefResolver
import com.example.biblia.domain.crossref.CrossRefTarget
import com.example.biblia.domain.model.HighlightColor.BLUE
import com.example.biblia.domain.model.HighlightColor.YELLOW
import com.example.biblia.domain.model.HighlightMark
import com.example.biblia.domain.model.HighlightPassage
import com.example.biblia.domain.model.HighlightPassages
import com.example.biblia.domain.model.NoteSort
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseSpans
import org.junit.Assert.assertEquals
import org.junit.Test

class HighlightPassagesTest {

    private fun m(b: Int, c: Int, v: Int, color: com.example.biblia.domain.model.HighlightColor = YELLOW, t: Long = 0) =
        HighlightMark(b, c, v, color, t)

    @Test
    fun consecutiveSameColourVersesBecomeOnePassage() {
        val passages = HighlightPassages.group(
            listOf(m(43, 3, 17, t = 5), m(43, 3, 16, t = 9), m(43, 3, 18, BLUE), m(43, 3, 20), m(43, 4, 1), m(1, 1, 1)),
        )
        assertEquals(
            listOf(
                HighlightPassage(1, 1, 1, 1, YELLOW, 0),
                HighlightPassage(43, 3, 16, 17, YELLOW, 9), // newest change of the two
                HighlightPassage(43, 3, 18, 18, BLUE, 0), // other colour breaks the run
                HighlightPassage(43, 3, 20, 20, YELLOW, 0), // a gap breaks it
                HighlightPassage(43, 4, 1, 1, YELLOW, 0), // never across chapters
            ),
            passages,
        )
        assertEquals(setOf(16, 17), passages[1].verses)
    }

    @Test
    fun aHighlightOnAMergedNumberShowsAsTheCombinedVerse() {
        // Highlighted on Ruth 1:2 in KJV; MBB12 combines 1-2.
        val p = HighlightPassages.group(listOf(m(8, 1, 2))).single()
        val mbbRuth1 = listOf(Verse(8, 1, 1, "Nang ang Israel..."), Verse(8, 1, 2, ""), Verse(8, 1, 3, "Namatay..."))
        val ends = VerseSpans.ends(mbbRuth1)
        val r = CrossRefResolver.resolve(CrossRefTarget(p.book, p.chapter, p.startVerse, p.chapter, p.endVerse), ends, ends)
        assertEquals("Ruth 1:1-2", CrossRefResolver.label("Ruth", r))
    }

    @Test
    fun filterByColourAndTextThenSort() {
        val items = HighlightPassages.group(listOf(m(43, 3, 16, YELLOW, 1), m(1, 1, 1, BLUE, 3), m(19, 23, 1, YELLOW, 2)))
        val text = mapOf(43 to "Juan 3:16 pag-ibig", 1 to "Genesis 1:1 nilikha", 19 to "Mga Awit 23:1 pastol")
        fun run(q: String, colors: Set<com.example.biblia.domain.model.HighlightColor>, sort: NoteSort) =
            HighlightPassages.filter(items, { it }, { text.getValue(it.book) }, q, colors, sort).map { it.book }
        assertEquals(listOf(1, 19, 43), run("", emptySet(), NoteSort.RECENT))
        assertEquals(listOf(1, 19, 43), run("", emptySet(), NoteSort.BIBLE))
        assertEquals(listOf(19, 43), run("", setOf(YELLOW), NoteSort.RECENT))
        assertEquals(listOf(43), run("PAG-ÍBIG", emptySet(), NoteSort.RECENT))
        assertEquals(listOf(19), run("awit", setOf(YELLOW), NoteSort.BIBLE))
    }
}
