package com.example.biblia

import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.domain.crossref.CrossRefParser
import com.example.biblia.domain.crossref.CrossRefTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class CrossRefParserTest {

    @Test
    fun osisAbbreviationsMapToTheAppsBookNumbers() {
        assertEquals(1, CrossRefParser.bookNumber("Gen"))
        assertEquals(19, CrossRefParser.bookNumber("Ps"))
        assertEquals(22, CrossRefParser.bookNumber("Song"))
        assertEquals(40, CrossRefParser.bookNumber("Matt"))
        assertEquals(46, CrossRefParser.bookNumber("1Cor"))
        assertEquals(57, CrossRefParser.bookNumber("Phlm"))
        assertEquals(62, CrossRefParser.bookNumber("1John"))
        assertEquals(66, CrossRefParser.bookNumber("Rev"))
        assertNull(CrossRefParser.bookNumber("Tob")) // apocrypha / unknown
    }

    @Test
    fun everyBookInTheRealFileIsMapped() {
        val file = listOf(File("src/main/assets/crossrefs/cross_references.txt"), File("app/src/main/assets/crossrefs/cross_references.txt"))
            .first { it.exists() }
        val books = HashSet<String>()
        file.useLines { lines -> lines.drop(1).forEach { l -> l.split('\t').take(2).forEach { r -> r.split('-').forEach { books += it.substringBefore('.') } } } }
        assertEquals(66, books.size)
        books.forEach { osis -> assertEquals(osis, true, CrossRefParser.bookNumber(osis) != null) }
        // Same numbering as the app's catalog (spot checks by English name).
        assertEquals("Psalms", BookCatalog.book(CrossRefParser.bookNumber("Ps")!!).englishName)
        assertEquals("1 Corinthians", BookCatalog.book(CrossRefParser.bookNumber("1Cor")!!).englishName)
    }

    @Test
    fun singleVersesAndRanges() {
        assertEquals(CrossRefTarget(19, 90, 2), CrossRefParser.parseTarget("Ps.90.2"))
        assertEquals(CrossRefTarget(20, 8, 22, 8, 30), CrossRefParser.parseTarget("Prov.8.22-Prov.8.30"))
        assertEquals(CrossRefTarget(1, 1, 31, 2, 3), CrossRefParser.parseTarget("Gen.1.31-Gen.2.3")) // crosses a chapter
        assertEquals(CrossRefTarget(1, 50, 26), CrossRefParser.parseTarget("Gen.50.26-Exod.1.1")) // other book: start only
        assertEquals(CrossRefTarget(1, 2, 5), CrossRefParser.parseTarget("Gen.2.5-Gen.2.1")) // backwards: start only
        assertNull(CrossRefParser.parseTarget("Gen.1"))
        assertNull(CrossRefParser.parseTarget("Xyz.1.1"))
    }

    @Test
    fun linesSkipHeaderBadAndNegativeVotes() {
        assertNull(CrossRefParser.parseLine("From Verse\tTo Verse\tVotes\t#www.openbible.info CC-BY 2026-11-23"))
        assertNull(CrossRefParser.parseLine("Gen.1.1\tJob.38.4\t-3"))
        assertNull(CrossRefParser.parseLine("Gen.1.1\tJob.38.4"))
        val row = CrossRefParser.parseLine("Gen.1.1\tRom.1.19-Rom.1.20\t60")!!
        assertEquals(Triple(1, 1, 1), Triple(row.book, row.chapter, row.verse))
        assertEquals(CrossRefTarget(45, 1, 19, 1, 20), row.target)
        assertEquals(60, row.votes)
    }

    @Test
    fun keepsTheTopTenPerVerseByVotes() {
        val lines = sequenceOf("From Verse\tTo Verse\tVotes") +
            (1..12).map { "Gen.1.1\tPs.$it.1\t$it" } +
            sequenceOf("Gen.1.1\tPs.99.1\t-50", "Gen.1.2\tJohn.1.1\t5")
        val groups = CrossRefParser.topPerSource(lines, limit = 10).toList()
        assertEquals(2, groups.size)
        assertEquals((12 downTo 3).toList(), groups[0].map { it.votes })
        assertEquals(listOf(5), groups[1].map { it.votes })
        assertEquals(1_001_002, CrossRefParser.key(1, 1, 2))
    }
}
