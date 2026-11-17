package com.example.biblia

import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.domain.model.BookFilter
import com.example.biblia.domain.search.BookSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM tests for instant book-name search and the cumulative flat table. */
class BookSearchTest {

    @Test
    fun `juan matches Juan first plus all three John epistles`() {
        val result = BookSearch.search("juan")
        assertEquals(43, result.first().number)
        val numbers = result.map { it.number }
        assertTrue(numbers.containsAll(listOf(43, 62, 63, 64)))
    }

    @Test
    fun `abbreviation and english name match`() {
        assertEquals(43, BookSearch.search("jn").first().number)
        assertEquals(43, BookSearch.search("john").first().number)
    }

    @Test
    fun `numbered books match with and without space`() {
        assertEquals(46, BookSearch.search("1 cor").first().number)
        assertEquals(46, BookSearch.search("1cor").first().number)
        assertEquals(46, BookSearch.search("1 corinto").first().number)
    }

    @Test
    fun `word prefix mga aw and plain awit rank mga awit first`() {
        assertEquals(19, BookSearch.search("mga aw").first().number)
        val byWord = BookSearch.search("awit")
        assertTrue(byWord.map { it.number }.containsAll(listOf(19, 22)))
        assertEquals(19, byWord.first().number)
    }

    @Test
    fun `accent and case insensitive`() {
        assertEquals(43, BookSearch.search("JUÁN").first().number)
        assertEquals(19, BookSearch.search("AWÍT").first().number)
    }

    @Test
    fun `new testament filter excludes genesis but keeps juan`() {
        assertTrue(BookSearch.search("genesis", BookFilter.NEW_TESTAMENT).isEmpty())
        assertTrue(BookSearch.search("gen", BookFilter.NEW_TESTAMENT).isEmpty())
        assertEquals(listOf(43, 62, 63, 64), BookSearch.search("juan", BookFilter.NEW_TESTAMENT).map { it.number })
    }

    @Test
    fun `no match and empty query return empty`() {
        assertTrue(BookSearch.search("xyzzy").isEmpty())
        assertTrue(BookSearch.search("").isEmpty())
        assertTrue(BookSearch.search("   ").isEmpty())
    }

    @Test
    fun `cumulative chapter offset table matches the canon`() {
        assertEquals(66, BookCatalog.standardChapterCounts.size)
        assertEquals(1189, BookCatalog.totalStandardChapters)
        assertEquals(0, BookCatalog.flatIndexOfStandard(1, 1))
        assertEquals(1188, BookCatalog.flatIndexOfStandard(66, 22))
        assertEquals(929, BookCatalog.flatIndexOfStandard(40, 1))
        assertEquals(999, BookCatalog.flatIndexOfStandard(43, 3))
        assertEquals(1187, BookCatalog.flatIndexOfStandard(66, 21))
        assertEquals(150, BookCatalog.standardChapterCount(19))
    }
}
