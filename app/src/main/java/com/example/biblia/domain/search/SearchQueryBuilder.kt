package com.example.biblia.domain.search

/** Builds SQLite FTS4 MATCH expressions from free-text queries. */
object SearchQueryBuilder {

    private val tokenSeparator = Regex("[^\\p{L}\\p{N}]+")

    /**
     * Builds an FTS4 MATCH expression from a free-text query. Every token
     * becomes a case/accent-insensitive prefix term restricted to the
     * `norm_text` column, e.g. "Mahal na Diyos" -> "norm_text:mahal* AND norm_text:na* AND norm_text:diyos*".
     *
     * @return the MATCH expression, or null when the query has no usable tokens.
     */
    fun buildMatchQuery(rawQuery: String): String? {
        val tokens = foldTokens(rawQuery)
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" AND ") { token -> "norm_text:$token*" }
    }

    /** Splits a raw query into folded, accent-stripped tokens. */
    fun foldTokens(rawQuery: String): List<String> =
        TextNormalizer.fold(rawQuery.lowercase())
            .split(tokenSeparator)
            .filter { it.isNotBlank() }
}
