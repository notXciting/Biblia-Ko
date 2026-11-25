package com.example.biblia.data.user

import com.example.biblia.domain.model.HighlightColor
import com.example.biblia.domain.model.HighlightMark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Verse highlights for the reader (version-independent). */
class HighlightRepository(private val dao: HighlightDao) {

    /** verse number -> color for one chapter; re-emits on every change. */
    fun observeChapter(book: Int, chapter: Int): Flow<Map<Int, HighlightColor>> =
        dao.observeChapter(book, chapter).map { rows ->
            rows.mapNotNull { row ->
                HighlightColor.entries.firstOrNull { it.name == row.color }?.let { row.verse to it }
            }.toMap()
        }

    /** Every highlight as a mark (unknown colours dropped), for "Aking Pag-aaral". */
    fun observeAll(): Flow<List<HighlightMark>> =
        dao.observeAll().map { rows ->
            rows.mapNotNull { row ->
                HighlightColor.entries.firstOrNull { it.name == row.color }
                    ?.let { HighlightMark(row.book, row.chapter, row.verse, it, row.updatedAt) }
            }
        }

    suspend fun set(book: Int, chapter: Int, verses: Collection<Int>, color: HighlightColor) {
        val now = System.currentTimeMillis()
        dao.upsert(verses.map { HighlightEntity(book, chapter, it, color.name, now) })
    }

    suspend fun clear(book: Int, chapter: Int, verses: Collection<Int>) {
        dao.delete(book, chapter, verses.toList())
    }
}
