package com.example.biblia.domain.model

/**
 * Combined verses. Some translations (MBB12, ASD) merge several verses into
 * one: the text sits on the first number and the following numbers are EMPTY
 * (Ruth 1:1 carries 1-2, then 3 follows). A verse with text "spans" through
 * every consecutive empty verse after it, also at the end of a chapter.
 * Verses a translation omits are not empty (they carry bracketed text).
 */
object VerseSpans {

    /** Text verse -> last verse it covers, ONLY for combined verses (span > 1). */
    fun ends(verses: List<Verse>): Map<Int, Int> {
        val out = HashMap<Int, Int>()
        var owner = 0
        for (v in verses) {
            if (!v.isEmpty) owner = v.verse
            else if (owner > 0) out[owner] = v.verse
        }
        return out
    }

    fun end(verse: Int, ends: Map<Int, Int>): Int = ends[verse] ?: verse

    /** "1-2" for a combined verse, else "1". */
    fun label(verse: Int, ends: Map<Int, Int>): String {
        val end = end(verse, ends)
        return if (end > verse) "$verse-$end" else verse.toString()
    }

    /** The text verse that holds [verse] (itself unless it is merged into an earlier one). */
    fun ownerOf(verse: Int, ends: Map<Int, Int>): Int =
        ends.entries.firstOrNull { (start, end) -> verse in (start + 1)..end }?.key ?: verse

    /** Every verse number covered by [verses], e.g. {1} -> {1, 2} when 1 spans 1-2. */
    fun covered(verses: Collection<Int>, ends: Map<Int, Int>): Set<Int> =
        verses.flatMapTo(sortedSetOf()) { it..end(it, ends) }
}
