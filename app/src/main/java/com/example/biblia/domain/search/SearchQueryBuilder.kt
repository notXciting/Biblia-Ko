package com.example.biblia.domain.search

/**
 * Builds SQLite FTS4 MATCH expressions from free-text queries.
 *
 * Android's SQLite uses the FTS *standard* query syntax: words separated by a
 * space are ANDed, and the keyword "AND" is NOT an operator there — it was
 * searched as the word "and", so every multi-word Tagalog query found nothing.
 * A column filter also can't prefix a quoted phrase in that syntax.
 */
object SearchQueryBuilder {

    private val tokenSeparator = Regex("[^\\p{L}\\p{N}]+")

    /**
     * Every word as a prefix term on `norm_text`, implicitly ANDed:
     * "Mahal na Diyos" -> "norm_text:mahal* norm_text:na* norm_text:diyos*".
     * A typed compound without its hyphen ("pagibig") matches the joined form
     * the index stores for "pag-ibig" (see [SearchIndexText]).
     *
     * @return the MATCH expression, or null when the query has no usable tokens.
     */
    fun buildMatchQuery(rawQuery: String): String? {
        val tokens = foldTokens(rawQuery)
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ") { token -> "norm_text:$token*" }
    }

    /**
     * The words as one adjacent phrase, last word as a prefix:
     * "pag-ibig" / "pag ibig" -> "\"pag ibig*\"". Its results are listed first,
     * so the real phrase ranks above verses that merely contain both words.
     * Null for a single word or when a word has digits (a phrase can't carry
     * the column filter, and digits could match the verse_id column).
     */
    fun buildPhraseQuery(rawQuery: String): String? {
        val tokens = foldTokens(rawQuery)
        if (tokens.size < 2 || tokens.any { t -> t.any { it.isDigit() } }) return null
        return "\"" + tokens.joinToString(" ") + "*\""
    }

    /** Splits a raw query into folded, accent-stripped tokens. */
    fun foldTokens(rawQuery: String): List<String> =
        TextNormalizer.fold(rawQuery.lowercase())
            .split(tokenSeparator)
            .filter { it.isNotBlank() }
}
