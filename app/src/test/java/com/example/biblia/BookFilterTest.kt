package com.example.biblia

import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.domain.model.BookFilter
import com.example.biblia.domain.model.FilterGroup
import com.example.biblia.domain.search.BookSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookFilterTest {

    @Test
    fun noFilterIsASingleBook() {
        BookFilter.entries.forEach { assertTrue("${it.name} has ${it.bookCount}", it.bookCount >= 3) }
    }

    @Test
    fun testamentsAndSectionsPartitionTheBible() {
        assertEquals(39, BookFilter.OLD_TESTAMENT.bookCount)
        assertEquals(27, BookFilter.NEW_TESTAMENT.bookCount)
        val otSections = BookFilter.entries.filter { it.group == FilterGroup.OLD_TESTAMENT }
        assertEquals((1..39).toList(), otSections.flatMap { it.books!! }.sorted())
    }

    @Test
    fun everyBookIsReachableThroughANonGeneralFilter() {
        val covered = BookFilter.entries.filter { it.group != FilterGroup.GENERAL }
            .flatMap { it.books!! }.toSet()
        assertEquals((1..66).toSet(), covered) // Acts via Narrative, Revelation via Prophecy
    }

    @Test
    fun oneChapterFilterMatchesTheChapterTable() {
        val expected = (1..66).filter { BookCatalog.standardChapterCount(it) == 1 }
        assertEquals(expected, BookFilter.ONE_CHAPTER.books)
    }

    @Test
    fun themeFiltersHaveTheRightBooks() {
        assertEquals(listOf(49, 50, 51, 57), BookFilter.PRISON_LETTERS.books)
        assertEquals(listOf(54, 55, 56), BookFilter.PASTORAL_LETTERS.books)
        assertTrue(BookFilter.PROPHECY.matches(66) && BookFilter.PROPHECY.matches(27))
        assertTrue(BookFilter.NARRATIVE.matches(44) && !BookFilter.NARRATIVE.matches(45))
    }

    @Test
    fun bookSearchRespectsNonContiguousFilters() {
        // "fil" -> Philippians (50) is a prison letter; Philemon (57) too.
        val hits = BookSearch.search("phil", BookFilter.PRISON_LETTERS).map { it.number }
        assertEquals(listOf(50, 57), hits.sorted())
    }
}
