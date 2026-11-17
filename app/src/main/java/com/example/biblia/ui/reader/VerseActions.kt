package com.example.biblia.ui.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.biblia.data.BibleVersion
import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.di.AppContainer
import com.example.biblia.domain.model.HighlightColor
import com.example.biblia.domain.model.Verse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** One version's text for the compare sheet; [verses] null = not installed. */
data class CompareRow(val version: BibleVersion, val verses: List<Verse>?)

/** Compare sheet state: the reference plus one row per registered version. */
data class CompareState(val reference: String, val rows: List<CompareRow>)

/**
 * Actions on a verse selection that go beyond the reader's own state:
 * highlights (user database) and comparing the selection across versions.
 * Owned by [ReaderViewModel], running on its scope.
 */
class VerseActions(private val container: AppContainer, private val scope: CoroutineScope) {

    private val highlightRepo = container.highlightRepository
    private var compareJob: Job? = null

    var compare by mutableStateOf<CompareState?>(null); private set

    fun highlights(book: Int, chapter: Int): Flow<Map<Int, HighlightColor>> =
        highlightRepo.observeChapter(book, chapter)

    fun highlight(book: Int, chapter: Int, verses: Set<Int>, color: HighlightColor) {
        scope.launch { highlightRepo.set(book, chapter, verses, color) }
    }

    fun clearHighlight(book: Int, chapter: Int, verses: Set<Int>) {
        scope.launch { highlightRepo.clear(book, chapter, verses) }
    }

    /** Loads the selected verses from every installed version (one query each). */
    fun openCompare(reference: String, book: Int, chapter: Int, verses: Set<Int>) {
        compareJob?.cancel()
        compareJob = scope.launch {
            val rows = BibleVersionRegistry.all.map { version ->
                val text = if (container.settingsRepository.isImported(version.id)) {
                    runCatching { container.bibleRepository.getChapter(version.id, book, chapter) }
                        .getOrNull()?.verses?.filter { it.verse in verses && !it.isEmpty }
                } else {
                    null
                }
                CompareRow(version, text)
            }
            compare = CompareState(reference, rows)
        }
    }

    fun closeCompare() {
        compareJob?.cancel()
        compare = null
    }
}
