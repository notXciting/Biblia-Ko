package com.example.biblia.domain.reference

import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.domain.model.VerseReference
import com.example.biblia.domain.search.TextNormalizer

/**
 * Parses typed references like "Juan 3:16", "Jn 3", "1 cor 13", "john 3 16"
 * into a [VerseReference]. Understands Tagalog names, English names and
 * abbreviations, with ":", ".", "-" or spaces between chapter and verse.
 *
 * Returns null when the input is not a reference — the caller should then fall
 * back to full-text search.
 */
object ReferenceParser {

    /**
     * Group 1: book name (optional leading chapter number + letters).
     * Group 2: chapter. Group 3 (optional): verse.
     */
    private val referenceRegex =
        Regex("""^\s*((?:\d+[\s.]*[\p{L}\s]*|[\p{L}\s]+)[\p{L}\s]*?)[\s.]*?(\d{1,3})(?:\s*[:.\-–—]?\s*(\d{1,3}))?\s*$""")

    private const val MAX_CHAPTER = 150
    private const val MAX_VERSE = 210

    fun parse(raw: String): VerseReference? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        // A reference always contains at least one digit (the chapter).
        if (trimmed.none { it.isDigit() }) return null

        val normalized = TextNormalizer.fold(trimmed.lowercase())
        val match = referenceRegex.find(normalized) ?: return null

        val name = match.groupValues[1].trim()
        if (name.isEmpty()) return null
        val chapter = match.groupValues[2].toIntOrNull() ?: return null
        val verse = match.groupValues[3].toIntOrNull()

        val book = BookCatalog.resolveAlias(name) ?: return null
        if (chapter < 1 || chapter > MAX_CHAPTER) return null
        if (verse != null && (verse < 1 || verse > MAX_VERSE)) return null
        return VerseReference(book.number, chapter, verse)
    }
}
