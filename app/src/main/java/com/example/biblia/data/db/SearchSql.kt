package com.example.biblia.data.db

/**
 * SQL for the full-text search index: ONE contentless FTS4 table per version
 * (`fts_<versionId>`, docid = verses.id). Pure strings, so JVM tests run them
 * against real SQLite.
 *
 * Why this layout (measured on the 10 bundled Bibles):
 * - contentless (`content=""`) stores only the index, not a second copy of
 *   the folded text: the database went from 166 MB to 83 MB;
 * - per version, search scans only that Bible's postings and needs no
 *   versionId filter: common words ("ang") are 2-3x faster;
 * - a contentless FTS4 table can't DELETE rows, so a version's index is
 *   dropped and rebuilt as a whole (re-import, index format change).
 *
 * Room doesn't manage these tables; [SearchIndex] creates and fills them.
 */
object SearchSql {

    private val safeId = Regex("[a-z0-9_]+")

    fun table(versionId: String): String {
        require(safeId.matches(versionId)) { "Unsafe version id: $versionId" }
        return "fts_$versionId"
    }

    fun create(versionId: String) = "CREATE VIRTUAL TABLE IF NOT EXISTS ${table(versionId)} USING fts4(content=\"\", norm_text)"

    fun drop(versionId: String) = "DROP TABLE IF EXISTS ${table(versionId)}"

    fun insert(versionId: String) = "INSERT INTO ${table(versionId)}(docid, norm_text) VALUES (?, ?)"

    /** Merges the index segments into one b-tree: smaller and faster to query. */
    fun optimize(versionId: String) = table(versionId).let { "INSERT INTO $it($it) VALUES('optimize')" }

    const val EXISTS = "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?"

    /**
     * Matches in canonical order, streaming: docids are verse ids, assigned
     * Genesis -> Revelation at import, and CROSS JOIN keeps the FTS table as the
     * outer loop, so SQLite stops at LIMIT instead of sorting every match.
     * Args: [searchArgs].
     */
    fun search(versionId: String, books: List<Int>?): String {
        val t = table(versionId)
        val bookFilter = books?.let { " AND v.book IN (" + it.joinToString(",") { "?" } + ")" }.orEmpty()
        return "SELECT v.book AS book, v.chapter AS chapter, v.verse AS verse, v.text AS text " +
            "FROM $t f CROSS JOIN verses v ON v.id = f.docid " +
            "WHERE $t MATCH ? AND v.text != ''$bookFilter " +
            "ORDER BY f.docid LIMIT ?"
    }

    fun searchArgs(match: String, books: List<Int>?, limit: Int): Array<Any> =
        (listOf<Any>(match) + books.orEmpty() + limit).toTypedArray()
}
