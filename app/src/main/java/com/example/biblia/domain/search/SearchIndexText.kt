package com.example.biblia.domain.search

/**
 * Text stored in the FTS index for one verse: the folded text, plus every
 * hyphenated word again WITHOUT its hyphens ("pag-ibig" -> also "pagibig",
 * "mag-asawa" -> "magasawa"). The tokenizer splits on hyphens, so without the
 * joined form a search for "pagibig" could never match. Snippets are built
 * from the original verse text, so the extra words are never shown.
 */
object SearchIndexText {

    /**
     * Bump when [of] or the index layout changes; installed Bibles are
     * re-indexed once on startup. 2 = joined compounds; 3 = per-version
     * contentless tables (data/db/SearchSql).
     */
    const val FORMAT = 3

    private val compound = Regex("[\\p{L}\\p{N}]+(?:[-‐‑][\\p{L}\\p{N}]+)+")
    private val hyphens = Regex("[-‐‑]")

    fun of(text: String): String {
        val folded = TextNormalizer.fold(text.lowercase())
        val joined = compound.findAll(folded).map { it.value.replace(hyphens, "") }.toList()
        return if (joined.isEmpty()) folded else folded + " " + joined.joinToString(" ")
    }

    /** Regex that finds [token] in folded text with optional hyphens between letters. */
    fun hyphenTolerant(token: String): Regex =
        Regex(token.map { Regex.escape(it.toString()) }.joinToString("[-‐‑]?"))
}
