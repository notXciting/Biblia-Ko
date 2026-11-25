package com.example.biblia

import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.domain.model.BookNameLanguage
import com.example.biblia.domain.model.EnglishBookShortNames
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The reader title falls back to these when the full book name doesn't fit. */
class BookShortNameTest {

    @After fun reset() { BookNameLanguage.english = false }

    @Test
    fun shortNamesFollowTheBibleLanguage() {
        val thess = BookCatalog.book(52)
        BookNameLanguage.english = false
        assertEquals("1 Tes", thess.displayShortName)
        BookNameLanguage.english = true
        assertEquals("1 Thess", thess.displayShortName)
        assertEquals("Ps", BookCatalog.book(19).displayShortName)
        assertEquals("Rev", BookCatalog.book(66).displayShortName)
    }

    @Test
    fun everyBookHasAShortNameNoLongerThanItsFullName() {
        for (n in 1..66) {
            val book = BookCatalog.book(n)
            assertTrue(n.toString(), EnglishBookShortNames.of(n).length <= book.englishName.length)
            assertTrue(n.toString(), book.abbreviation.isNotBlank())
        }
    }
}
