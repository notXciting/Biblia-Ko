package com.example.biblia

import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.data.parser.BibleMetadata
import com.example.biblia.data.parser.BibleParserListener
import com.example.biblia.data.parser.TagalogXmlBibleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kxml2.io.KXmlParser
import java.io.File

/**
 * Every registered version's asset must exist and parse with the shared
 * parser into the canonical 66 books / 1189 chapters.
 */
class EnglishBiblesParserTest {

    private class Counter : BibleParserListener {
        var books = 0
        var chapters = 0
        var verses = 0
        var john316 = ""
        var metadata: BibleMetadata? = null
        private var book = 0
        private var chapter = 0
        override fun onMetadata(metadata: BibleMetadata) { this.metadata = metadata }
        override fun onBookStart(number: Int) { books++; book = number }
        override fun onChapterStart(number: Int) { chapters++; chapter = number }
        override fun onVerse(number: Int, text: String) {
            verses++
            if (book == 43 && chapter == 3 && number == 16) john316 = text
        }
    }

    private fun asset(path: String): File =
        listOf(File("src/main/assets/$path"), File("app/src/main/assets/$path"))
            .firstOrNull { it.exists() } ?: error("Asset not found: $path")

    private fun parse(path: String): Counter {
        val counter = Counter()
        asset(path).inputStream().use { TagalogXmlBibleParser { KXmlParser() }.parse(it, counter) }
        return counter
    }

    @Test
    fun everyRegisteredVersionParsesToCanonicalStructure() {
        for (version in BibleVersionRegistry.all) {
            val c = parse(version.assetPath)
            assertEquals(version.id + " books", 66, c.books)
            assertEquals(version.id + " chapters", 1189, c.chapters)
            assertTrue(version.id + " verses", c.verses > 30_000)
            assertTrue(version.id + " John 3:16 present", c.john316.isNotBlank())
        }
    }

    @Test
    fun amplifiedNameAttributeIsUsedAsTranslation() {
        val c = parse("bibles/EnglishAmplifiedBible.xml")
        assertTrue(c.metadata!!.translation.startsWith("English Amplified"))
    }

    @Test
    fun englishVersionsHaveRegistryCopyright() {
        BibleVersionRegistry.all.filter { it.language == "English" }.forEach {
            assertTrue(it.id, it.copyright.contains("Copyright"))
        }
    }
}
