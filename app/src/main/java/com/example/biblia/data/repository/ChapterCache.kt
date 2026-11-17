package com.example.biblia.data.repository

import com.example.biblia.domain.model.ChapterContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Shared LRU chapter cache used by the reader's pager AND by the selection
 * screen's pre-jump load. [peek] is synchronous so a page whose chapter is
 * already cached renders its text in the very first frame (no blank page).
 */
class ChapterCache(private val repo: BibleRepository) {

    private val cache = object : LinkedHashMap<String, ChapterContent>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ChapterContent>): Boolean =
            size > MAX_ENTRIES
    }

    private fun key(versionId: String, book: Int, chapter: Int) = "$versionId:$book:$chapter"

    /** Cached chapter or null; never touches the database. */
    fun peek(versionId: String, book: Int, chapter: Int): ChapterContent? =
        synchronized(cache) { cache[key(versionId, book, chapter)] }

    suspend fun get(versionId: String, book: Int, chapter: Int): ChapterContent {
        val k = key(versionId, book, chapter)
        synchronized(cache) { cache[k] }?.let { return it }
        val content = repo.getChapter(versionId, book, chapter)
        synchronized(cache) { cache[k] = content }
        return content
    }

    /** Fire-and-forget preload on the given background scope. */
    fun preload(scope: CoroutineScope, versionId: String, book: Int, chapter: Int) {
        if (peek(versionId, book, chapter) != null) return
        scope.launch {
            runCatching { get(versionId, book, chapter) }
        }
    }

    companion object {
        // A chapter is a few KB of text; 24 keeps the recent neighborhood warm.
        private const val MAX_ENTRIES = 24
    }
}
