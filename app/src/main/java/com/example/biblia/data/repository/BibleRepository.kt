package com.example.biblia.data.repository

import com.example.biblia.data.db.BibleDao
import com.example.biblia.domain.model.BookFilter
import com.example.biblia.domain.model.ChapterContent
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VersionMeta
import com.example.biblia.domain.search.SearchQueryBuilder
import java.util.concurrent.ConcurrentHashMap

/** Read API over the Room database; the UI never touches the DAO directly. */
class BibleRepository(private val dao: BibleDao) {

    private val chapterCountsCache = ConcurrentHashMap<String, Map<Int, Int>>()

    suspend fun getChapter(versionId: String, book: Int, chapter: Int): ChapterContent {
        val entities = dao.getChapter(versionId, book, chapter)
        return ChapterContent(
            versionId = versionId,
            book = book,
            chapter = chapter,
            verses = entities.map { Verse(it.book, it.chapter, it.verse, it.text) },
        )
    }

    /** book number -> number of chapters, for the given version. */
    suspend fun chapterCounts(versionId: String): Map<Int, Int> {
        chapterCountsCache[versionId]?.let { return it }
        val counts = dao.chapterCounts(versionId).associate { it.book to it.maxChapter }
        chapterCountsCache[versionId] = counts
        return counts
    }

    /** In-memory chapter/verse count table: key = book * 1000L + chapter. */
    suspend fun verseCounts(versionId: String): Map<Long, Int> =
        dao.verseCounts(versionId).associate { (it.book * 1000L + it.chapter) to it.maxVerse }

    suspend fun getChapterCount(versionId: String, book: Int): Int =
        chapterCounts(versionId)[book] ?: 0

    suspend fun getVerseCount(versionId: String, book: Int, chapter: Int): Int =
        dao.maxVerse(versionId, book, chapter) ?: 0

    suspend fun search(
        versionId: String,
        query: String,
        filter: BookFilter = BookFilter.ALL,
        limit: Int = 100,
    ): List<Verse> {
        val matchQuery = SearchQueryBuilder.buildMatchQuery(query) ?: return emptyList()
        val books = filter.books
        // A non-contiguous filter (e.g. Prison Letters) needs IN, not BETWEEN.
        return dao.search(versionId, matchQuery, books == null, books ?: listOf(0), limit)
            .map { Verse(it.book, it.chapter, it.verse, it.text) }
    }

    suspend fun versionMeta(versionId: String): VersionMeta? =
        dao.versionMeta(versionId)?.let {
            VersionMeta(it.versionId, it.translation, it.copyright, it.link)
        }

    /** Called after an import so cached chapter counts are refreshed. */
    fun invalidateCounts(versionId: String) {
        chapterCountsCache.remove(versionId)
    }
}
