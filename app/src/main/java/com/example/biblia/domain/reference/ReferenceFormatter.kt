package com.example.biblia.domain.reference

/** Formats a verse selection as a compact reference: "Juan 3:16-18, 20". */
object ReferenceFormatter {

    fun format(bookName: String, chapter: Int, verses: Collection<Int>): String {
        val sorted = verses.toSortedSet()
        if (sorted.isEmpty()) return "$bookName $chapter"
        return "$bookName $chapter:" + ranges(sorted)
    }

    /** Collapses consecutive numbers into runs: [1,2,3,5,7,8] -> "1-3, 5, 7-8". */
    fun ranges(sorted: Collection<Int>): String {
        val parts = ArrayList<String>()
        var start = -1
        var prev = -1
        for (n in sorted) {
            if (start < 0) {
                start = n
            } else if (n != prev + 1) {
                parts.add(run(start, prev))
                start = n
            }
            prev = n
        }
        if (start >= 0) parts.add(run(start, prev))
        return parts.joinToString(", ")
    }

    private fun run(a: Int, b: Int) = if (a == b) "$a" else "$a-$b"
}
