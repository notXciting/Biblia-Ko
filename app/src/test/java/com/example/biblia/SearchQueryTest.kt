package com.example.biblia

import com.example.biblia.domain.search.SearchIndexText
import com.example.biblia.domain.search.SearchQueryBuilder
import com.example.biblia.domain.search.SnippetBuilder
import com.example.biblia.domain.search.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** "pag-ibig" must be findable as "pag-ibig", "pag ibig" and "pagibig". */
class SearchQueryTest {

    @Test
    fun wordsAreImplicitlyAndedNeverWithTheAndKeyword() {
        // Android FTS uses the standard syntax: "AND" would be searched as a word.
        val q = SearchQueryBuilder.buildMatchQuery("Pag-ibig ng Diyos")
        assertEquals("norm_text:pag* norm_text:ibig* norm_text:ng* norm_text:diyos*", q)
        assertFalse(q!!.contains("AND"))
        assertEquals("norm_text:pagibig*", SearchQueryBuilder.buildMatchQuery("pagibig"))
        assertNull(SearchQueryBuilder.buildMatchQuery(" - "))
    }

    @Test
    fun hyphenAndSpaceGiveTheSamePhrase() {
        assertEquals("\"pag ibig*\"", SearchQueryBuilder.buildPhraseQuery("pag-ibig"))
        assertEquals("\"pag ibig*\"", SearchQueryBuilder.buildPhraseQuery("  Pag   Ibig "))
        assertNull(SearchQueryBuilder.buildPhraseQuery("pagibig")) // one word: terms only
        assertNull(SearchQueryBuilder.buildPhraseQuery("juan 3")) // digits never in a phrase
    }

    @Test
    fun indexAlsoStoresHyphenatedWordsJoined() {
        val text = SearchIndexText.of("Ang PAG-IBIG ay matiyaga; ang mag-asawa ay nagmamahalan.")
        assertTrue(text.startsWith("ang pag-ibig ay matiyaga; ang mag-asawa ay nagmamahalan."))
        assertTrue(text.endsWith(" pagibig magasawa"))
        assertEquals("walang gitling", SearchIndexText.of("Walang gitling"))
        // Non-breaking hyphen (U+2011) counts too; accents are folded.
        assertTrue(SearchIndexText.of("pag‑íbig").endsWith(" pagibig"))
    }

    @Test
    fun snippetHighlightsHyphenatedWordForJoinedQuery() {
        val verse = "Sapagkat gayon na lamang ang pag-ibig ng Diyos sa sanlibutan"
        val snippet = SnippetBuilder.build(verse, SearchQueryBuilder.foldTokens("pagibig"))
        val highlighted = snippet.highlightRanges.map { snippet.text.substring(it.first, it.last + 1) }
        assertEquals(listOf("pag-ibig"), highlighted)
    }

    @Test
    fun foldKeepsLengthAndStripsAccents() {
        val raw = "Ñandú PAG-ÍBIG"
        assertEquals("nandu pag-ibig", TextNormalizer.fold(raw))
        assertEquals(raw.length, TextNormalizer.fold(raw).length)
    }
}
