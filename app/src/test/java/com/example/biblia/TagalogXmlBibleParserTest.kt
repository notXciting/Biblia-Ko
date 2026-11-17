package com.example.biblia

import com.example.biblia.data.parser.BibleMetadata
import com.example.biblia.data.parser.BibleParserListener
import com.example.biblia.data.parser.BomSupport
import com.example.biblia.data.parser.TagalogXmlBibleParser
import com.example.biblia.domain.search.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kxml2.io.KXmlParser
import java.io.File

/**
 * Parses the REAL Tagalog2012Bible.xml (workspace root) and asserts the exact
 * expected structure: 66 books, 1189 chapters, 30692 verses, 179 empty verses.
 */
class TagalogXmlBibleParserTest {

    private class Collector : BibleParserListener {
        var books = 0
        var chapters = 0
        var verses = 0
        var emptyVerses = 0
        var firstVerse: String? = null
        var lastBook = 0
        var lastChapter = 0
        var metadata: BibleMetadata? = null

        override fun onMetadata(metadata: BibleMetadata) {
            this.metadata = metadata
        }

        override fun onBookStart(number: Int) {
            books++
            lastBook = number
        }

        override fun onChapterStart(number: Int) {
            chapters++
            lastChapter = number
        }

        override fun onVerse(number: Int, text: String) {
            verses++
            if (text.isBlank()) emptyVerses++
            if (firstVerse == null) firstVerse = text
        }
    }

    private fun realFile(): File {
        val candidates = listOf(
            File("../Tagalog2012Bible.xml"),
            File("Tagalog2012Bible.xml"),
            File("../../Tagalog2012Bible.xml"),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Tagalog2012Bible.xml not found. Tried: ${candidates.map { it.absolutePath }}")
    }

    private fun parseRealFile(): Collector {
        val parser = TagalogXmlBibleParser { KXmlParser() }
        val collector = Collector()
        realFile().inputStream().use { raw ->
            parser.parse(BomSupport.stripBom(raw), collector)
        }
        return collector
    }

    @Test
    fun `real file parses with exactly 66 books 1189 chapters 30692 verses 179 empty`() {
        val collector = parseRealFile()
        assertEquals(66, collector.books)
        assertEquals(1189, collector.chapters)
        assertEquals(30692, collector.verses)
        assertEquals(179, collector.emptyVerses)
    }

    @Test
    fun `real file spans Genesis 1 to Revelation 22`() {
        val collector = parseRealFile()
        // First parsed verse is Genesis 1:1; last parsed chapter belongs to
        // book 66 (Revelation, which has 22 chapters).
        assertTrue(collector.firstVerse.orEmpty().startsWith("Nang pasimula"))
        assertEquals(66, collector.lastBook)
        assertEquals(22, collector.lastChapter)
    }

    @Test
    fun `metadata contains translation and copyright`() {
        val collector = parseRealFile()
        assertEquals("Tagalog 2012 (Magandang Balita Biblia)", collector.metadata?.translation)
        assertTrue(
            "copyright should mention Philippine Bible Society",
            collector.metadata?.copyright?.contains("Philippine Bible Society") == true,
        )
        assertTrue(collector.metadata?.link?.startsWith("https://") == true)
    }

    @Test
    fun `bom and crlf are handled`() {
        val bytes = realFile().readBytes()
        assertEquals(0xEF, bytes[0].toInt() and 0xFF)
        assertEquals(0xBB, bytes[1].toInt() and 0xFF)
        assertEquals(0xBF, bytes[2].toInt() and 0xFF)
        // Parsing succeeds end-to-end despite the BOM (see other tests) and the
        // first verse text contains no BOM character.
        val collector = parseRealFile()
        assertTrue(!collector.firstVerse.orEmpty().contains('\uFEFF'))
    }

    @Test
    fun `fold is accent and case insensitive and length preserving`() {
        assertEquals("nandu", TextNormalizer.fold("Ñandú"))
        assertEquals("hello world", TextNormalizer.fold("HELLO WORLD"))
        assertEquals(3, TextNormalizer.fold("ñāñ").length)
        assertEquals("san dios", TextNormalizer.fold("SaÑ Díos"))
        assertEquals("1cor", TextNormalizer.normalizeName("1 Cor. "))
    }
}
