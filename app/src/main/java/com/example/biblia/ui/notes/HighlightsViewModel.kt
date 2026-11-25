package com.example.biblia.ui.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.di.AppContainer
import com.example.biblia.di.openInReader
import com.example.biblia.domain.crossref.CrossRefResolver
import com.example.biblia.domain.crossref.CrossRefTarget
import com.example.biblia.domain.crossref.ResolvedRef
import com.example.biblia.domain.model.ChapterContent
import com.example.biblia.domain.model.HighlightColor
import com.example.biblia.domain.model.HighlightPassage
import com.example.biblia.domain.model.HighlightPassages
import com.example.biblia.domain.model.NoteSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One row of "Mga Marka": the passage, as shown in the Bible being read. */
data class HighlightItem(
    val passage: HighlightPassage,
    /** Widened to whole combined verses of the current version (Ruth 1:1-2). */
    val shown: ResolvedRef,
    val label: String,
    val text: String,
)

/**
 * "Mga Marka": every highlight grouped into passages, with text from the Bible
 * in use (chapters read once per version and cached), colour filter, and
 * change colour / remove / go to verse.
 */
class HighlightsViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.highlightRepository
    private val chapters = HashMap<String, ChapterContent?>()

    var colors by mutableStateOf<Set<HighlightColor>>(emptySet()); private set

    /** null until the first emission (no flash of "no highlights"). */
    val items: StateFlow<List<HighlightItem>?> = combine(
        repo.observeAll(),
        container.settingsRepository.settings.map { it.versionId }.distinctUntilChanged(),
    ) { marks, versionId -> HighlightPassages.group(marks).mapNotNull { build(it, versionId) } }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private suspend fun build(p: HighlightPassage, versionId: String): HighlightItem? {
        val key = "$versionId:${p.book}:${p.chapter}"
        val content = chapters.getOrPut(key) {
            runCatching { container.bibleRepository.getChapter(versionId, p.book, p.chapter) }.getOrNull()
        }
        val ends = content?.spanEnds.orEmpty()
        val shown = CrossRefResolver.resolve(CrossRefTarget(p.book, p.chapter, p.startVerse, p.chapter, p.endVerse), ends, ends)
        val text = CrossRefResolver.text(mapOf(p.chapter to content?.verses.orEmpty()), shown)
        val label = CrossRefResolver.label(BookCatalog.book(p.book).displayName, shown)
        return HighlightItem(p, shown, label, text)
    }

    fun visible(all: List<HighlightItem>, query: String, sort: NoteSort): List<HighlightItem> =
        HighlightPassages.filter(all, { it.passage }, { it.label + " " + it.text }, query, colors, sort)

    fun toggleColor(color: HighlightColor) {
        colors = if (color in colors) colors - color else colors + color
    }

    fun changeColor(item: HighlightItem, color: HighlightColor) {
        viewModelScope.launch { repo.set(item.passage.book, item.passage.chapter, item.passage.verses, color) }
    }

    fun remove(item: HighlightItem) {
        viewModelScope.launch { repo.clear(item.passage.book, item.passage.chapter, item.passage.verses) }
    }

    /** Opens the passage in the reader; [onReady] pops back to it. */
    fun goTo(item: HighlightItem, onReady: () -> Unit) {
        viewModelScope.launch {
            container.openInReader(container.settingsRepository.initial().versionId, item.shown.book, item.shown.chapter, item.shown.verse)
            onReady()
        }
    }
}
