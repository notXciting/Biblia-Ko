package com.example.biblia.domain.search

/** A shortened verse text plus the ranges (relative to [text]) to highlight. */
data class VerseSnippet(val text: String, val highlightRanges: List<IntRange>)

/**
 * Builds search-result snippets with highlight ranges. Matching is done on the
 * length-preserving fold, so ranges computed on the folded text map directly
 * onto the original text (accent-insensitive highlighting).
 */
object SnippetBuilder {

    private const val CONTEXT_BEFORE = 40

    fun build(text: String, tokens: List<String>, maxLength: Int = 180): VerseSnippet {
        if (text.isEmpty()) return VerseSnippet("", emptyList())
        if (tokens.isEmpty()) return VerseSnippet(text.take(maxLength), emptyList())

        val folded = TextNormalizer.fold(text.lowercase())

        // Collect every occurrence of every token; "pagibig" also finds "pag-ibig".
        val occurrences = ArrayList<IntRange>()
        for (token in tokens) {
            SearchIndexText.hyphenTolerant(token).findAll(folded).forEach { occurrences.add(it.range) }
        }
        if (occurrences.isEmpty()) return VerseSnippet(text.take(maxLength), emptyList())

        // Sort and drop overlapping ranges.
        val ranges = ArrayList<IntRange>()
        for (range in occurrences.sortedBy { it.first }) {
            if (ranges.isEmpty() || range.first > ranges.last().last) ranges.add(range)
        }

        // Build a window around the first match.
        val first = ranges.first().first
        var start = (first - CONTEXT_BEFORE).coerceAtLeast(0)
        if (start > 0) {
            val space = folded.lastIndexOf(' ', start)
            if (space > 0) start = space + 1
        }
        var end = (start + maxLength).coerceAtMost(text.length)
        if (end < text.length) {
            val space = folded.indexOf(' ', end)
            if (space > end) end = space
        }

        val prefix = if (start > 0) "… " else ""
        val suffix = if (end < text.length) " …" else ""
        val snippetText = prefix + text.substring(start, end) + suffix

        val snippetRanges = ranges.mapNotNull { range ->
            val s = range.first - start + prefix.length
            val e = range.last + 1 - start + prefix.length
            val clampedS = s.coerceAtLeast(0)
            val clampedE = e.coerceAtMost(snippetText.length)
            if (clampedE > clampedS && clampedE > prefix.length) clampedS until clampedE else null
        }
        return VerseSnippet(snippetText, snippetRanges)
    }
}
