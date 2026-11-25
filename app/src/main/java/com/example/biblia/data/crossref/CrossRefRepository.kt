package com.example.biblia.data.crossref

import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.data.repository.BibleRepository
import com.example.biblia.domain.crossref.CrossRefResolver
import com.example.biblia.domain.crossref.CrossRefTarget
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseSpans
import kotlinx.coroutines.flow.StateFlow

/** One related passage, ready to show: label + text in the current version. */
data class CrossRefItem(
    val label: String,
    val text: String,
    val book: Int,
    val chapter: Int,
    val verse: Int,
)

/**
 * Cross-references for a verse selection, resolved and filled with text from
 * the Bible version in use. Chapters are read straight from Room (not the
 * reader's ChapterCache, which would evict the pages being read).
 */
class CrossRefRepository(
    private val dao: CrossRefDao,
    private val bible: BibleRepository,
    val ready: StateFlow<Boolean>,
) {

    /** Best-voted first, duplicates removed; empty until the import is done. */
    suspend fun forSelection(versionId: String, book: Int, chapter: Int, verses: Set<Int>, limit: Int = LIMIT): List<CrossRefItem> {
        if (!ready.value || verses.isEmpty()) return emptyList()
        val rows = dao.forSources(CrossRefResolver.sourceKeys(book, chapter, verses), limit * 2)
        val targets = rows.map { CrossRefTarget(it.toBook, it.toChapter, it.toVerse, it.toEndChapter, it.toEndVerse) }
            .distinct()
            .filterNot { it.book == book && it.chapter == chapter && it.endChapter == chapter && it.verse in verses }
        val chapters = HashMap<Pair<Int, Int>, List<Verse>>()
        suspend fun load(b: Int, c: Int) = chapters.getOrPut(b to c) { runCatching { bible.getChapter(versionId, b, c).verses }.getOrDefault(emptyList()) }

        val items = ArrayList<CrossRefItem>()
        for (t in targets) {
            if (items.size >= limit) break
            val lastChapter = minOf(t.endChapter, t.chapter + MAX_CHAPTERS - 1)
            val loaded = (t.chapter..lastChapter).associateWith { load(t.book, it) }
            // A range cut short at MAX_CHAPTERS ends with the last verse of the last shown chapter.
            val endVerse = if (lastChapter == t.endChapter) t.endVerse
            else loaded[lastChapter].orEmpty().maxOfOrNull { it.verse } ?: 1
            val resolved = CrossRefResolver.resolve(
                t.copy(endChapter = lastChapter, endVerse = endVerse),
                VerseSpans.ends(loaded[t.chapter].orEmpty()),
                VerseSpans.ends(loaded[lastChapter].orEmpty()),
            )
            val text = CrossRefResolver.text(loaded, resolved)
            if (text.isBlank()) continue // e.g. a verse this version omits
            val label = CrossRefResolver.label(BookCatalog.book(t.book).displayName, resolved)
            if (items.none { it.label == label }) items += CrossRefItem(label, text, t.book, resolved.chapter, resolved.verse)
        }
        return items
    }

    private companion object {
        const val LIMIT = 10
        /** Long ranges ("Gen.1.1-Gen.11.9") show at most this many chapters of text. */
        const val MAX_CHAPTERS = 2
    }
}
