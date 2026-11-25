package com.example.biblia.data.crossref

import android.content.Context
import android.util.Log
import com.example.biblia.BuildConfig
import com.example.biblia.domain.crossref.CrossRefParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Imports assets/crossrefs/cross_references.txt into [CrossRefDatabase] once,
 * in the background (the UI just shows no cross-references until [ready]).
 * Streams the file line by line, keeps the top [PER_VERSE] refs per verse by
 * votes (negative votes skipped) and inserts in batched transactions.
 */
class CrossRefImporter(private val context: Context, private val database: CrossRefDatabase) {

    private val mutex = Mutex()
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    suspend fun ensureImported() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val dao = database.crossRefDao()
            if (dao.format() == FORMAT) {
                _ready.value = true
                return@withLock
            }
            val started = System.currentTimeMillis()
            dao.clear() // partial data from an interrupted import
            val batch = ArrayList<CrossRefEntity>(BATCH)
            fun flush() {
                if (batch.isEmpty()) return
                database.runInTransaction { dao.insertAll(batch) }
                batch.clear()
            }
            context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).useLines { lines ->
                CrossRefParser.topPerSource(lines, PER_VERSE).forEach { rows ->
                    rows.forEach { r ->
                        val t = r.target
                        batch += CrossRefEntity(
                            sourceKey = CrossRefParser.key(r.book, r.chapter, r.verse),
                            toBook = t.book, toChapter = t.chapter, toVerse = t.verse,
                            toEndChapter = t.endChapter, toEndVerse = t.endVerse, votes = r.votes,
                        )
                    }
                    if (batch.size >= BATCH) flush()
                }
            }
            flush()
            dao.setMeta(CrossRefMetaEntity(format = FORMAT))
            _ready.value = true
            if (BuildConfig.DEBUG) Log.d(TAG, "Imported in " + (System.currentTimeMillis() - started) + " ms")
        }
    }

    private companion object {
        const val TAG = "CrossRefImporter"
        const val ASSET = "crossrefs/cross_references.txt"
        const val PER_VERSE = 10
        const val BATCH = 2_000
        /** Bump to re-import after changing what is stored. */
        const val FORMAT = 1
    }
}
