package com.example.biblia.domain.search

import java.text.Normalizer

/**
 * Text folding helpers shared by the FTS index, the search query builder and
 * the snippet highlighter.
 */
object TextNormalizer {

    /**
     * Accent- and case-folds text while preserving the character count
     * (one input char -> one output char), so match offsets computed on the
     * folded text are valid on the original text.
     *
     * Examples: "Ñandú" -> "nandu", "HELLO" -> "hello".
     */
    fun fold(text: String): String {
        if (text.isEmpty()) return text
        val sb = StringBuilder(text.length)
        for (c in text) {
            if (c.code < 128) { sb.append(c.lowercaseChar()); continue } // no accents to strip
            val lower = c.lowercaseChar()
            val decomposed = Normalizer.normalize(lower.toString(), Normalizer.Form.NFD)
            sb.append(if (decomposed.isEmpty()) lower else decomposed[0])
        }
        return sb.toString()
    }

    /**
     * Normalizes a book name or alias for matching: folded, lowercase, with
     * dots and ALL whitespace removed ("1 Cor." -> "1cor").
     */
    fun normalizeName(raw: String): String =
        fold(raw.lowercase()).replace(Regex("[.\\s]+"), "")
}
