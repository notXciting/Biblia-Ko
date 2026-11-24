package com.example.biblia.data.db

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.biblia.domain.search.SearchIndexText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

/**
 * The per-version contentless FTS4 search tables ([SearchSql]). All calls run
 * on IO threads; writes go through Room's single writer connection, so they
 * can join a `runInTransaction` block of the importer.
 */
class SearchIndex(private val database: BibleDatabase) {

    private val db: SupportSQLiteDatabase get() = database.openHelper.writableDatabase
    private val known = ConcurrentHashMap.newKeySet<String>()

    private val _pending = MutableStateFlow<Set<String>>(emptySet())

    /** Installed versions whose index is still being (re)built: search shows "preparing". */
    val pending: StateFlow<Set<String>> = _pending.asStateFlow()

    fun markPending(versionIds: Collection<String>) = _pending.update { it + versionIds }
    fun markReady(versionId: String) = _pending.update { it - versionId }

    fun exists(versionId: String): Boolean {
        if (versionId in known) return true
        val found = db.query(SearchSql.EXISTS, arrayOf(SearchSql.table(versionId))).use { it.moveToFirst() }
        if (found) known += versionId
        return found
    }

    /** Drops [versionId]'s index (re-import or rebuild). */
    fun drop(versionId: String) {
        known -= versionId
        db.execSQL(SearchSql.drop(versionId))
    }

    /** Empty index for [versionId], replacing any old one. */
    fun recreate(versionId: String) {
        drop(versionId)
        db.execSQL(SearchSql.create(versionId))
        known += versionId
    }

    /** Indexes verses by id (docid = verses.id keeps canonical order). */
    fun insert(versionId: String, ids: List<Long>, texts: List<String>) {
        val statement = db.compileStatement(SearchSql.insert(versionId))
        statement.use {
            for (i in ids.indices) {
                it.bindLong(1, ids[i])
                it.bindString(2, SearchIndexText.of(texts[i]))
                it.executeInsert()
                it.clearBindings()
            }
        }
    }

    fun optimize(versionId: String) = db.execSQL(SearchSql.optimize(versionId))

    /** Rebuilds [versionId]'s index from the verses already in Room (no re-import). */
    fun rebuildFromVerses(versionId: String, batch: Int) {
        val rows = database.bibleDao().verseTexts(versionId)
        database.runInTransaction {
            recreate(versionId)
            rows.chunked(batch).forEach { chunk -> insert(versionId, chunk.map { it.id }, chunk.map { it.text }) }
        }
        optimize(versionId)
    }

    /**
     * Gives freed pages back to the file system and truncates the write-ahead
     * log (both otherwise keep their peak size forever). Best effort.
     */
    fun compact(vacuum: Boolean) {
        runCatching { if (vacuum) db.execSQL("VACUUM") }
        runCatching { db.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() } }
    }

    /** Empty while the version's index doesn't exist yet (see [pending]). */
    suspend fun search(versionId: String, match: String, books: List<Int>?, limit: Int): List<VerseSearchRow> {
        if (!exists(versionId)) return emptyList()
        val query = SimpleSQLiteQuery(SearchSql.search(versionId, books), SearchSql.searchArgs(match, books, limit))
        return database.bibleDao().search(query)
    }
}
