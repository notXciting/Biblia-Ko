package com.example.biblia.data.parser

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/**
 * Streaming XmlPullParser implementation for the Tagalog2012Bible.xml schema:
 *
 * <bible translation="..." status="..." link="...">
 *   <testament name="Old|New">
 *     <book number="1..66">
 *       <chapter number="1..n">
 *         <verse number="1..n">text</verse>
 *
 * Never builds a DOM: events are pushed straight to the listener. The BOM is
 * stripped before parsing, and the 179 empty <verse/> elements are emitted as
 * empty strings so callers can render them dimmed / skip them.
 */
class TagalogXmlBibleParser(
    private val parserFactory: () -> XmlPullParser = { Xml.newPullParser() },
) : BibleParser {

    override fun parse(input: InputStream, listener: BibleParserListener) {
        val parser = parserFactory()
        runCatching { parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false) }
        parser.setInput(BomSupport.stripBom(input), "UTF-8")

        var inVerse = false
        var verseNumber = 0
        val text = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    TAG_BIBLE -> listener.onMetadata(
                        BibleMetadata(
                            // Some files (e.g. Amplified) use name= instead of translation=.
                            translation = (parser.getAttributeValue(null, ATTR_TRANSLATION)
                                ?: parser.getAttributeValue(null, ATTR_NAME)).orEmpty(),
                            copyright = parser.getAttributeValue(null, ATTR_STATUS).orEmpty(),
                            link = parser.getAttributeValue(null, ATTR_LINK).orEmpty(),
                        )
                    )
                    TAG_BOOK -> listener.onBookStart(
                        parser.getAttributeValue(null, ATTR_NUMBER)?.toIntOrNull() ?: 0
                    )
                    TAG_CHAPTER -> listener.onChapterStart(
                        parser.getAttributeValue(null, ATTR_NUMBER)?.toIntOrNull() ?: 0
                    )
                    TAG_VERSE -> {
                        inVerse = true
                        verseNumber = parser.getAttributeValue(null, ATTR_NUMBER)?.toIntOrNull() ?: 0
                        text.setLength(0)
                    }
                }
                XmlPullParser.TEXT -> if (inVerse) text.append(parser.text)
                XmlPullParser.END_TAG -> if (parser.name == TAG_VERSE && inVerse) {
                    listener.onVerse(verseNumber, text.toString().trim())
                    inVerse = false
                }
            }
            event = parser.next()
        }
    }

    private companion object {
        const val TAG_BIBLE = "bible"
        const val TAG_TESTAMENT = "testament"
        const val TAG_BOOK = "book"
        const val TAG_CHAPTER = "chapter"
        const val TAG_VERSE = "verse"
        const val ATTR_NUMBER = "number"
        const val ATTR_TRANSLATION = "translation"
        const val ATTR_NAME = "name"
        const val ATTR_STATUS = "status"
        const val ATTR_LINK = "link"
    }
}
