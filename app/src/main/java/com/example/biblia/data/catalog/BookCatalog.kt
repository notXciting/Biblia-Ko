package com.example.biblia.data.catalog

import com.example.biblia.domain.model.BookInfo
import com.example.biblia.domain.search.TextNormalizer

/**
 * Static catalog of all 66 books (the Bible XML carries no book names).
 * Also owns the alias map used by the reference parser and the canonical
 * chapter-count table used for O(1) flat pager-index math.
 */
object BookCatalog {

    val books: List<BookInfo> = BookCatalogData.entries

    val byNumber: Map<Int, BookInfo> = books.associateBy { it.number }

    fun book(number: Int): BookInfo = byNumber[number] ?: books.first()

    fun count(): Int = books.size

    /** Canonical chapter counts for books 1..66 (identical for every version). */
    val standardChapterCounts: IntArray = intArrayOf(
        50, 40, 27, 36, 34, 24, 21, 4, 31, 24,
        22, 25, 29, 36, 10, 13, 10, 42, 150, 31,
        12, 8, 66, 52, 5, 48, 12, 14, 3, 9,
        1, 4, 7, 3, 3, 3, 2, 14, 4,
        28, 16, 24, 21, 28, 16, 16, 13, 6, 6,
        4, 4, 5, 3, 6, 4, 3, 1, 13, 5,
        5, 3, 5, 1, 1, 1, 22,
    )

    /** Cumulative flat start index of chapter 1 of each book (flatStart[0] = 0). */
    val flatStart: IntArray by lazy {
        val starts = IntArray(books.size)
        var running = 0
        for (i in starts.indices) {
            starts[i] = running
            running += standardChapterCounts[i]
        }
        starts
    }

    val totalStandardChapters: Int by lazy { standardChapterCounts.sum() }

    /** O(1) flat pager index (0..1188) from book/chapter via the precomputed table. */
    fun flatIndexOfStandard(book: Int, chapter: Int): Int {
        val b = book.coerceIn(1, books.size)
        return flatStart[b - 1] + (chapter.coerceAtLeast(1) - 1)
    }

    /** Standard chapter count of a book (for clamping without a DB query). */
    fun standardChapterCount(book: Int): Int =
        standardChapterCounts[book.coerceIn(1, books.size) - 1]

    /**
     * Alias -> book number. Keys are normalized with [TextNormalizer.normalizeName]
     * (folded, lowercase, dots and spaces removed), so "1 Cor.", "1cor" and
     * "1 cor" all resolve identically.
     */
    val aliases: Map<String, Int> by lazy {
        val map = HashMap<String, Int>()
        fun put(alias: String, number: Int) {
            val key = TextNormalizer.normalizeName(alias)
            if (key.isNotEmpty()) map[key] = number
        }
        for (b in books) {
            put(b.tagalogName, b.number)
            put(b.englishName, b.number)
            put(b.abbreviation, b.number)
        }
        // Common extra aliases (English variants + popular short forms).
        put("Ps", 19); put("Psalm", 19); put("Psalms", 19); put("Awit", 19)
        put("Prov", 20); put("Pro", 20)
        put("Ecc", 21); put("Eccl", 21)
        put("Song", 22); put("Song of Songs", 22); put("Canticles", 22)
        put("Lam", 25)
        put("Eze", 26)
        put("Hos", 28)
        put("Oba", 31)
        put("Mic", 33)
        put("Zep", 36)
        put("Zech", 38); put("Zec", 38)
        put("Matt", 40)
        put("Mrk", 41)
        put("Lk", 42)
        put("Jhn", 43); put("Joh", 43)
        put("Act", 44)
        put("Eph", 49)
        put("Php", 50); put("Phil", 50)
        put("1 Th", 52); put("2 Th", 53)
        put("1 Ti", 54); put("2 Ti", 55)
        put("Phlm", 57)
        put("Jas", 59); put("Jam", 59)
        put("1 Pet", 60); put("2 Pet", 61)
        put("Rv", 66)
        put("Josh", 6); put("Jdg", 7); put("Judg", 7)
        put("Num", 4); put("Deut", 5)
        put("Ex", 2); put("Exod", 2)
        put("1 Chr", 13); put("2 Chr", 14)
        put("Ezr", 15)
        map
    }

    /** Resolves a typed book name (any language/abbreviation) to a book, or null. */
    fun resolveAlias(rawName: String): BookInfo? {
        val key = TextNormalizer.normalizeName(rawName)
        if (key.isEmpty()) return null
        return aliases[key]?.let { book(it) }
    }
}
