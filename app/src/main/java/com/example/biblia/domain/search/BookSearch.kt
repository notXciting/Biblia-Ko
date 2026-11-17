package com.example.biblia.domain.search

import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.domain.model.BookFilter
import com.example.biblia.domain.model.BookInfo

/**
 * Instant, synchronous book-name search (pure Kotlin, no Android dependencies).
 *
 * Matches Tagalog name, English name and abbreviation; case- and
 * accent-insensitive via [TextNormalizer]; numbered books work with or without
 * a space ("1 cor", "1cor", "1 corinto").
 *
 * Ranking: exact (1) > prefix (2) > word-prefix (3, "aw" matches "Mga Awit")
 * > contains (4). Ties resolve by book order.
 */
object BookSearch {

    /** @return matching books, best first, at most [limit] entries. */
    fun search(
        query: String,
        filter: BookFilter = BookFilter.ALL,
        limit: Int = Int.MAX_VALUE,
    ): List<BookInfo> {
        val q = TextNormalizer.normalizeName(query)
        if (q.isEmpty()) return emptyList()
        data class Scored(val book: BookInfo, val score: Int)
        val matches = ArrayList<Scored>()
        for (book in BookCatalog.books) {
            if (!filter.matches(book.number)) continue
            val score = bestScore(book, q)
            if (score > 0) matches.add(Scored(book, score))
        }
        matches.sortWith(compareBy({ it.score }, { it.book.number }))
        return matches.take(limit).map { it.book }
    }

    private fun bestScore(book: BookInfo, q: String): Int {
        var best = 0
        for (alias in listOf(book.tagalogName, book.englishName, book.abbreviation)) {
            val compact = TextNormalizer.normalizeName(alias)
            val spaced = TextNormalizer.fold(alias.lowercase())
                .replace('.', ' ')
                .replace(Regex("\\s+"), " ")
                .trim()
            val score = when {
                compact == q -> 1
                compact.startsWith(q) -> 2
                spaced.split(' ').any { it.startsWith(q) } -> 3
                spaced.contains(q) -> 4
                else -> 0
            }
            if (score != 0 && (best == 0 || score < best)) best = score
        }
        return best
    }
}
