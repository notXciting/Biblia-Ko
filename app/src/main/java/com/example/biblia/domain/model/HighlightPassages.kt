package com.example.biblia.domain.model

import com.example.biblia.domain.search.TextNormalizer

/** One highlighted verse (user.db stores highlights per verse). */
data class HighlightMark(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val color: HighlightColor,
    val updatedAt: Long,
)

/** Consecutive verses of one colour in one chapter, as the highlights list shows them. */
data class HighlightPassage(
    val book: Int,
    val chapter: Int,
    val startVerse: Int,
    val endVerse: Int,
    val color: HighlightColor,
    /** The most recent change of any verse in the passage. */
    val updatedAt: Long,
) {
    /** The stored verse numbers (what Change colour / Remove act on). */
    val verses: Set<Int> get() = (startVerse..endVerse).toSet()
}

/** Pure rules of the "Mga Marka" tab (unit-tested). */
object HighlightPassages {

    /** Joins consecutive same-colour verses of a chapter into passages (John 3:16 + 3:17 -> 3:16-17). */
    fun group(marks: List<HighlightMark>): List<HighlightPassage> {
        val out = ArrayList<HighlightPassage>()
        for (m in marks.sortedWith(compareBy({ it.book }, { it.chapter }, { it.verse }))) {
            val last = out.lastOrNull()
            if (last != null && last.book == m.book && last.chapter == m.chapter &&
                last.color == m.color && m.verse == last.endVerse + 1
            ) {
                out[out.lastIndex] = last.copy(endVerse = m.verse, updatedAt = maxOf(last.updatedAt, m.updatedAt))
            } else {
                out += HighlightPassage(m.book, m.chapter, m.verse, m.verse, m.color, m.updatedAt)
            }
        }
        return out
    }

    /**
     * The visible list: passages of the chosen [colors] (none chosen = all)
     * whose reference or text contains every word of [query], newest first or
     * in Bible order.
     */
    fun <T> filter(
        items: List<T>,
        passage: (T) -> HighlightPassage,
        searchable: (T) -> String,
        query: String,
        colors: Set<HighlightColor>,
        sort: NoteSort,
    ): List<T> {
        val words = TextNormalizer.fold(query.lowercase()).split(' ').filter { it.isNotBlank() }
        val found = items.filter { item ->
            val p = passage(item)
            (colors.isEmpty() || p.color in colors) &&
                (words.isEmpty() || TextNormalizer.fold(searchable(item).lowercase()).let { h -> words.all { it in h } })
        }
        return when (sort) {
            NoteSort.RECENT -> found.sortedByDescending { passage(it).updatedAt }
            NoteSort.BIBLE -> found.sortedWith(compareBy({ passage(it).book }, { passage(it).chapter }, { passage(it).startVerse }))
        }
    }
}
