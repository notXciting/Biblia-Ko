package com.example.biblia.domain.crossref

/**
 * A cross-reference target, language-independent: start verse plus the end of
 * the range (same verse for a single verse). Ranges may cross a chapter
 * ("Gen.1.31-Gen.2.3"), never a book.
 */
data class CrossRefTarget(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val endChapter: Int = chapter,
    val endVerse: Int = verse,
)

/** One line of the OpenBible.info file: source verse -> target, with its votes. */
data class CrossRefRow(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val target: CrossRefTarget,
    val votes: Int,
)

/**
 * Parser for OpenBible.info's cross_references.txt (CC BY 4.0, based on the
 * Treasury of Scripture Knowledge): a header line, then tab-separated
 * "From Verse / To Verse / Votes" with OSIS-style refs ("Gen.1.1",
 * "Prov.8.22-Prov.8.30"). Pure Kotlin, unit-tested.
 */
object CrossRefParser {

    /** OSIS abbreviations in canonical order: index + 1 = this app's book number. */
    private val OSIS = listOf(
        "Gen", "Exod", "Lev", "Num", "Deut", "Josh", "Judg", "Ruth", "1Sam", "2Sam",
        "1Kgs", "2Kgs", "1Chr", "2Chr", "Ezra", "Neh", "Esth", "Job", "Ps", "Prov",
        "Eccl", "Song", "Isa", "Jer", "Lam", "Ezek", "Dan", "Hos", "Joel", "Amos",
        "Obad", "Jonah", "Mic", "Nah", "Hab", "Zeph", "Hag", "Zech", "Mal",
        "Matt", "Mark", "Luke", "John", "Acts", "Rom", "1Cor", "2Cor", "Gal", "Eph",
        "Phil", "Col", "1Thess", "2Thess", "1Tim", "2Tim", "Titus", "Phlm", "Heb", "Jas",
        "1Pet", "2Pet", "1John", "2John", "3John", "Jude", "Rev",
    )
    private val bookNumbers: Map<String, Int> = OSIS.withIndex().associate { (i, name) -> name to i + 1 }

    /** OSIS abbreviation -> book number 1..66, or null. */
    fun bookNumber(osis: String): Int? = bookNumbers[osis]

    /** "Gen.1.1" -> (1, 1, 1), or null when malformed or unknown. */
    fun parseVerse(ref: String): Triple<Int, Int, Int>? {
        val parts = ref.trim().split('.')
        if (parts.size != 3) return null
        val book = bookNumber(parts[0]) ?: return null
        val chapter = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
        val verse = parts[2].toIntOrNull()?.takeIf { it > 0 } ?: return null
        return Triple(book, chapter, verse)
    }

    /**
     * "Ps.90.2" or "Prov.8.22-Prov.8.30" / "Gen.1.31-Gen.2.3". A range that
     * ends in another book (18 lines) or runs backwards keeps its start verse.
     */
    fun parseTarget(ref: String): CrossRefTarget? {
        val dash = ref.indexOf('-')
        val start = parseVerse(if (dash < 0) ref else ref.substring(0, dash)) ?: return null
        val end = if (dash < 0) null else parseVerse(ref.substring(dash + 1))
        val single = CrossRefTarget(start.first, start.second, start.third)
        if (end == null || end.first != start.first) return single
        val forward = end.second > start.second || (end.second == start.second && end.third >= start.third)
        return if (forward) single.copy(endChapter = end.second, endVerse = end.third) else single
    }

    /** One data line, or null for the header, blank/malformed lines and negative votes. */
    fun parseLine(line: String): CrossRefRow? {
        val cols = line.split('\t')
        if (cols.size < 3) return null
        val votes = cols[2].trim().toIntOrNull() ?: return null
        if (votes < 0) return null
        val (book, chapter, verse) = parseVerse(cols[0]) ?: return null
        val target = parseTarget(cols[1]) ?: return null
        return CrossRefRow(book, chapter, verse, target, votes)
    }

    /**
     * Streams [lines] into one list per source verse, keeping its [limit]
     * best-voted rows (ties keep file order). The file lists each source's
     * rows consecutively, so memory stays at one verse's rows.
     */
    fun topPerSource(lines: Sequence<String>, limit: Int = 10): Sequence<List<CrossRefRow>> = sequence {
        var current = ArrayList<CrossRefRow>()
        for (line in lines) {
            val row = parseLine(line) ?: continue
            val first = current.firstOrNull()
            if (first != null && (first.book != row.book || first.chapter != row.chapter || first.verse != row.verse)) {
                yield(current.sortedByDescending { it.votes }.take(limit))
                current = ArrayList()
            }
            current.add(row)
        }
        if (current.isNotEmpty()) yield(current.sortedByDescending { it.votes }.take(limit))
    }

    /** Index key of a source verse: book * 1_000_000 + chapter * 1_000 + verse. */
    fun key(book: Int, chapter: Int, verse: Int): Int = book * 1_000_000 + chapter * 1_000 + verse
}
