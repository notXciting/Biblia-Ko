package com.example.biblia

import com.example.biblia.data.db.SearchSql
import com.example.biblia.domain.search.SearchIndexText
import com.example.biblia.domain.search.SearchQueryBuilder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * Runs the real search-index SQL ([SearchSql]) against real SQLite with FTS4:
 * per-version contentless tables, docid = verse id, canonical order, phrase
 * and joined-compound matches, book filter and LIMIT.
 */
class SearchIndexSqlTest {

    private lateinit var db: Connection

    // book, chapter, verse, text (inserted in canonical order, like the importer)
    private val mbb = listOf(
        Triple(1, 1, 1) to "Nang pasimula ay nilikha ng Diyos ang langit at ang lupa.",
        Triple(8, 1, 1) to "Nang ang Israel ay pinamumunuan pa ng mga hukom...",
        Triple(8, 1, 2) to "", // merged into 1:1 (combined verse)
        Triple(43, 3, 16) to "Sapagkat gayon na lamang ang pag-ibig ng Diyos sa sanlibutan.",
        Triple(46, 13, 4) to "Ang pag-ibig ay matiyaga at magandang-loob.",
        Triple(62, 4, 8) to "Ang hindi umiibig ay hindi nakakakilala sa Diyos, sapagkat ang Diyos ay pag-ibig.",
    )
    private val kjv = listOf(
        Triple(43, 3, 16) to "For God so loved the world, that he gave his only begotten Son.",
    )

    @Before
    fun setUp() {
        db = DriverManager.getConnection("jdbc:sqlite::memory:")
        db.createStatement().use {
            it.execute(
                "CREATE TABLE verses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, versionId TEXT NOT NULL, " +
                    "book INTEGER NOT NULL, chapter INTEGER NOT NULL, verse INTEGER NOT NULL, text TEXT NOT NULL)"
            )
        }
        import("mbb12", mbb)
        import("kjv", kjv)
    }

    @After fun tearDown() = db.close()

    private fun exec(sql: String) = db.createStatement().use { it.execute(sql) }

    private fun import(versionId: String, rows: List<Pair<Triple<Int, Int, Int>, String>>) {
        exec(SearchSql.create(versionId))
        for ((ref, text) in rows) {
            val id = db.prepareStatement(
                "INSERT INTO verses(versionId, book, chapter, verse, text) VALUES (?,?,?,?,?)",
                java.sql.Statement.RETURN_GENERATED_KEYS,
            ).use { st ->
                st.setString(1, versionId); st.setInt(2, ref.first); st.setInt(3, ref.second)
                st.setInt(4, ref.third); st.setString(5, text)
                st.executeUpdate()
                st.generatedKeys.use { it.next(); it.getLong(1) }
            }
            db.prepareStatement(SearchSql.insert(versionId)).use { st ->
                st.setLong(1, id); st.setString(2, SearchIndexText.of(text)); st.executeUpdate()
            }
        }
        exec(SearchSql.optimize(versionId))
    }

    /** "book:chapter:verse" of each hit, in result order. */
    private fun search(versionId: String, match: String?, books: List<Int>? = null, limit: Int = 201): List<String> {
        match ?: return emptyList()
        return db.prepareStatement(SearchSql.search(versionId, books)).use { st ->
            SearchSql.searchArgs(match, books, limit).forEachIndexed { i, arg -> st.setObject(i + 1, arg) }
            st.executeQuery().use { rs ->
                buildList { while (rs.next()) add("${rs.getInt("book")}:${rs.getInt("chapter")}:${rs.getInt("verse")}") }
            }
        }
    }

    private fun words(q: String) = SearchQueryBuilder.buildMatchQuery(q)
    private fun phrase(q: String) = SearchQueryBuilder.buildPhraseQuery(q)

    @Test
    fun multiWordAndPhraseQueriesFindPagIbigInCanonicalOrder() {
        val expected = listOf("43:3:16", "46:13:4", "62:4:8")
        assertEquals(expected, search("mbb12", phrase("pag-ibig")))
        assertEquals(expected, search("mbb12", phrase("pag ibig")))
        assertEquals(expected, search("mbb12", words("pagibig"))) // joined compound
        assertEquals(listOf("1:1:1", "43:3:16", "62:4:8"), search("mbb12", words("Diyos")))
        assertEquals(listOf("46:13:4"), search("mbb12", words("magandangloob")))
    }

    @Test
    fun eachVersionOnlySearchesItsOwnIndex() {
        assertEquals(listOf("43:3:16"), search("kjv", words("loved world")))
        assertTrue(search("kjv", words("Diyos")).isEmpty())
        assertTrue(search("mbb12", words("loved")).isEmpty())
    }

    @Test
    fun bookFilterLimitAndEmptyVersesAreRespected() {
        assertEquals(listOf("62:4:8"), search("mbb12", words("Diyos"), books = listOf(62, 66)))
        assertEquals(listOf("1:1:1", "43:3:16"), search("mbb12", words("Diyos"), limit = 2))
        assertTrue(search("mbb12", words("Israel")).single() == "8:1:1") // empty 8:1:2 never listed
    }

    @Test
    fun indexIsContentlessAndDroppedAsAWhole() {
        val tables = db.createStatement().use { st ->
            st.executeQuery("SELECT name FROM sqlite_master").use { rs -> buildList { while (rs.next()) add(rs.getString(1)) } }
        }
        assertTrue("fts_mbb12" in tables)
        assertFalse("contentless: no copy of the text", "fts_mbb12_content" in tables)
        exec(SearchSql.drop("mbb12"))
        val left = db.prepareStatement(SearchSql.EXISTS).use { st ->
            st.setString(1, "fts_mbb12"); st.executeQuery().use { it.next() }
        }
        assertFalse(left)
        assertEquals(listOf("43:3:16"), search("kjv", words("world"))) // other versions untouched
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsafeVersionIdsAreRejected() {
        SearchSql.table("mbb12; DROP TABLE verses")
    }
}
