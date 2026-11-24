package com.example.biblia.ui.selection

import android.util.Log
import com.example.biblia.BuildConfig
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.domain.search.BookSearch
import com.example.biblia.di.AppContainer
import com.example.biblia.di.OpenVerseRequest
import com.example.biblia.domain.model.BookFilter
import com.example.biblia.domain.model.BookInfo
import com.example.biblia.domain.model.SearchScope
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseReference
import com.example.biblia.domain.model.ViewMode
import com.example.biblia.domain.reference.ReferenceParser
import com.example.biblia.perf.VersePerf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "VERSE_NAV"

enum class SelectionLevel { BOOKS, CHAPTERS, VERSES }

/** Selection screen state: drill-down, filter, scoped search, view mode. */
class SelectionViewModel(
    private val container: AppContainer,
    initialSettings: com.example.biblia.domain.model.AppSettings,
) : ViewModel() {

    private val repo = container.bibleRepository
    private val settingsRepo = container.settingsRepository
    private val navigationBus = container.navigationBus
    private var searchJob: Job? = null

    var version by mutableStateOf(BibleVersionRegistry.byId(initialSettings.versionId)); private set
    var viewMode by mutableStateOf(initialSettings.viewMode); private set
    var searchScope by mutableStateOf(initialSettings.searchScope); private set
    var level by mutableStateOf(SelectionLevel.BOOKS); private set
    var selectedBook by mutableStateOf(1); private set
    var selectedChapter by mutableStateOf(1); private set
    var filter by mutableStateOf(BookFilter.ALL); private set
    var query by mutableStateOf(""); private set
    var searchActive by mutableStateOf(false); private set
    var searching by mutableStateOf(false); private set
    var results by mutableStateOf<List<Verse>>(emptyList()); private set
    var truncated by mutableStateOf(false); private set
    var reference by mutableStateOf<VerseReference?>(null); private set
    var bookResults by mutableStateOf<List<BookInfo>>(emptyList()); private set
    var currentBook by mutableStateOf(1); private set
    var currentChapter by mutableStateOf(1); private set
    var currentVerse by mutableStateOf(0); private set
    var chapterCounts by mutableStateOf<Map<Int, Int>>(emptyMap()); private set

    /** This Bible's search index is still being rebuilt (after an app update). */
    var indexPending by mutableStateOf(false); private set

    init {
        viewModelScope.launch {
            val s = settingsRepo.initial()
            currentBook = s.book
            currentChapter = s.chapter
            currentVerse = s.verse
            selectedBook = s.book
            selectedChapter = s.chapter
            chapterCounts = repo.chapterCounts(version.id)
        }
        viewModelScope.launch {
            container.searchIndex.pending.collect { pending ->
                val wasPending = indexPending
                indexPending = version.id in pending
                // Index just finished: run the typed search for real.
                if (wasPending && !indexPending && searchActive && query.isNotBlank()) refreshSearch()
            }
        }
    }

    fun filteredBooks(): List<BookInfo> {
        val f = filter
        return if (f == BookFilter.ALL) BookCatalog.books
        else BookCatalog.books.filter { f.matches(it.number) }
    }

    fun chapterCountOf(book: Int): Int = chapterCounts[book] ?: BookCatalog.standardChapterCount(book)

    fun pickBook(number: Int) {
        selectedBook = number
        level = SelectionLevel.CHAPTERS
    }

    fun pickChapter(chapter: Int) {
        selectedChapter = chapter
        level = SelectionLevel.VERSES
    }

    /** Opens a chapter of the selected book directly in the reader. */
    fun openChapter(chapter: Int, onReady: () -> Unit) =
        jumpTo(selectedBook, chapter, 0, onReady)

    /** Opens the picked verse of the selected chapter in the reader. */
    fun pickVerse(verse: Int, onReady: () -> Unit) =
        jumpTo(selectedBook, selectedChapter, verse, onReady)

    /** Opens a search-result verse (its own book/chapter) in the reader. */
    fun pickSearchResult(verse: Verse, onReady: () -> Unit) =
        jumpTo(verse.book, verse.chapter, verse.verse, onReady)

    /** Opens a parsed reference (smart-jump card) in the reader. */
    fun openReference(ref: VerseReference, onReady: () -> Unit) =
        jumpTo(ref.book, ref.chapter, ref.verse ?: 0, onReady)

    /** True while a jump is being prepared; ignores double taps. */
    private var jumping = false

    /**
     * Loads the target chapter into the shared cache FIRST (a single indexed
     * query, typically a few ms; capped so a slow disk never blocks), then
     * publishes the request and pops. The reader therefore renders the target
     * chapter's text in its very first frame instead of a blank page.
     */
    private fun jumpTo(book: Int, chapter: Int, verse: Int, onReady: () -> Unit) {
        if (jumping) return
        jumping = true
        // A running full-text search can hold a DB connection for a long time
        // (short prefixes match most verses of every installed version).
        // Cancelling interrupts the SQLite query so the chapter load runs now.
        searchJob?.cancel()
        searching = false
        navLog("jumpTo book=$book chapter=$chapter verse=$verse")
        viewModelScope.launch {
            val cache = container.chapterCache
            if (cache.peek(version.id, book, chapter) == null) {
                withTimeoutOrNull(JUMP_LOAD_TIMEOUT_MS) {
                    runCatching { cache.get(version.id, book, chapter) }
                }
            }
            VersePerf.stage("target chapter cached")
            preloadNeighbors(book, chapter)
            navigationBus.open(OpenVerseRequest(version.id, book, chapter, verse))
            onReady()
            jumping = false
        }
    }

    /** Warms the adjacent chapters on IO so swiping after the jump is instant. */
    private fun preloadNeighbors(book: Int, chapter: Int) {
        val count = chapterCountOf(book)
        if (chapter > 1) {
            container.chapterCache.preload(container.applicationScope, version.id, book, chapter - 1)
        }
        if (chapter < count) {
            container.chapterCache.preload(container.applicationScope, version.id, book, chapter + 1)
        }
    }

    fun goBooks() {
        level = SelectionLevel.BOOKS
    }

    fun goChapters() {
        level = SelectionLevel.CHAPTERS
    }

    /** @return true when an upward level was consumed, false at the top level. */
    fun goUp(): Boolean = when (level) {
        SelectionLevel.VERSES -> { level = SelectionLevel.CHAPTERS; true }
        SelectionLevel.CHAPTERS -> { level = SelectionLevel.BOOKS; true }
        SelectionLevel.BOOKS -> false
    }

    fun selectFilter(value: BookFilter) {
        filter = value
        if (searchActive && query.isNotBlank()) refreshSearch()
    }

    fun selectScope(value: SearchScope) {
        if (searchScope == value) return
        searchScope = value
        viewModelScope.launch { settingsRepo.setSearchScope(value) }
        if (searchActive && query.isNotBlank()) refreshSearch()
    }

    fun toggleSearch(active: Boolean) {
        searchActive = active
        if (!active) {
            query = ""
            results = emptyList()
            truncated = false
            reference = null
            bookResults = emptyList()
            searching = false
        }
    }

    /**
     * Reference parsing is INSTANT (synchronous, no debounce, no Room); the FTS
     * text search keeps its 300 ms debounce and is skipped entirely when the
     * query is a valid reference.
     */
    fun updateQuery(value: String) {
        query = value
        refreshSearch()
    }

    private fun refreshSearch() {
        val q = query
        reference = runCatching { ReferenceParser.parse(q) }.getOrNull()
        bookResults = if (q.isBlank() || searchScope == SearchScope.TEXT) {
            emptyList()
        } else {
            BookSearch.search(q, filter, if (searchScope == SearchScope.ALL) BOOK_SUGGESTION_LIMIT else Int.MAX_VALUE)
        }
        searchJob?.cancel()
        // 1-letter queries ("j*") match nearly everything: very slow, never useful.
        val tooShort = q.count { it.isLetterOrDigit() } < MIN_FTS_CHARS
        if (q.isBlank() || tooShort || reference != null || searchScope == SearchScope.BOOKS) {
            results = emptyList()
            truncated = false
            searching = false
            return
        }
        searching = true
        searchJob = viewModelScope.launch {
            delay(300)
            runFtsNow()
        }
    }

    private suspend fun runFtsNow() {
        val found = withContext(Dispatchers.IO) {
            runCatching { repo.search(version.id, query, filter, SEARCH_LIMIT + 1) }
                .getOrDefault(emptyList())
        }
        truncated = found.size > SEARCH_LIMIT
        results = found.take(SEARCH_LIMIT)
        searching = false
    }

    fun toggleViewMode() {
        val next = if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
        viewMode = next
        viewModelScope.launch { settingsRepo.setViewMode(next) }
    }

    /** Goes through the shared cache so the reader can reuse this load. */
    suspend fun versesFor(book: Int, chapter: Int): List<Verse> =
        container.chapterCache.get(version.id, book, chapter).verses

    companion object {
        private const val SEARCH_LIMIT = 200
        private const val BOOK_SUGGESTION_LIMIT = 5
        private const val JUMP_LOAD_TIMEOUT_MS = 400L
        private const val MIN_FTS_CHARS = 2
    }
}


private fun navLog(message: String) {
    if (BuildConfig.DEBUG) Log.d(TAG, message)
}
