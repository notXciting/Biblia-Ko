package com.example.biblia

import com.example.biblia.domain.model.VerseReference
import com.example.biblia.domain.reference.ReferenceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Reference parsing: Tagalog names, English names, abbreviations. */
class ReferenceParserTest {

    @Test
    fun `tagalog full reference`() {
        assertEquals(VerseReference(43, 3, 16), ReferenceParser.parse("Juan 3:16"))
    }

    @Test
    fun `tagalog chapter only`() {
        assertEquals(VerseReference(43, 3), ReferenceParser.parse("juan 3"))
    }

    @Test
    fun `abbreviation chapter`() {
        assertEquals(VerseReference(43, 3), ReferenceParser.parse("jn 3"))
    }

    @Test
    fun `numbered book abbreviation`() {
        assertEquals(VerseReference(46, 13), ReferenceParser.parse("1 cor 13"))
        assertEquals(VerseReference(46, 13), ReferenceParser.parse("1cor 13"))
        assertEquals(VerseReference(46, 13, 4), ReferenceParser.parse("1 cor. 13:4"))
    }

    @Test
    fun `space separated chapter and verse`() {
        assertEquals(VerseReference(43, 3, 16), ReferenceParser.parse("john 3 16"))
    }

    @Test
    fun `book with leading number in multiple languages`() {
        assertEquals(VerseReference(11, 3), ReferenceParser.parse("1 mga hari 3"))
        assertEquals(VerseReference(11, 3), ReferenceParser.parse("1 hari 3"))
        assertEquals(VerseReference(11, 3), ReferenceParser.parse("1 kings 3"))
        assertEquals(VerseReference(63, 1, 4), ReferenceParser.parse("2 jn 1:4"))
    }

    @Test
    fun `psalms in tagalog and english`() {
        assertEquals(VerseReference(19, 23), ReferenceParser.parse("awit 23"))
        assertEquals(VerseReference(19, 23), ReferenceParser.parse("psalm 23"))
        assertEquals(VerseReference(19, 119, 105), ReferenceParser.parse("psalms 119:105"))
    }

    @Test
    fun `revelation`() {
        assertEquals(VerseReference(66, 21, 1), ReferenceParser.parse("rev 21:1"))
        assertEquals(VerseReference(66, 21), ReferenceParser.parse("pahayag 21"))
    }

    @Test
    fun `accents and case are ignored`() {
        assertEquals(VerseReference(48, 2, 20), ReferenceParser.parse("GALACIA 2:20"))
    }

    @Test
    fun `non references fall back to search`() {
        assertNull(ReferenceParser.parse("mahal na diyos"))
        assertNull(ReferenceParser.parse(""))
        assertNull(ReferenceParser.parse("   "))
        assertNull(ReferenceParser.parse("genesis"))
        assertNull(ReferenceParser.parse("3 16"))
        assertNull(ReferenceParser.parse("juan 316"))
    }
}
