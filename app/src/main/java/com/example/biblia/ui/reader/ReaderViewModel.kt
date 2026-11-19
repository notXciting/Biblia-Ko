package com.example.biblia.ui.reader

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblia.BuildConfig
import com.example.biblia.data.BibleVersion
import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.di.AppContainer
import com.example.biblia.domain.model.BookNameLanguage
import com.example.biblia.domain.model.ChapterContent
import com.example.biblia.domain.model.HighlightColor
import com.example.biblia.domain.reference.ReferenceFormatter
import com.example.biblia.domain.tts.TtsStatus
import com.example.biblia.domain.model.VerseReference
import com.example.biblia.domain.model.VerseSpans
import com.example.biblia.domain.model.VersionMeta
import com.example.biblia.perf.VersePerf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "VERSE_NAV"

/** Reader state: position, paging, selection, copy/share text, version switching. */
class ReaderViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.bibleRepository
    private val settingsRepo = container.settingsRepository
    private val chapterCache = container.chapterCache

    /** Highlights + compare-across-versions for the verse selection. */
    val actions = VerseActions(container, viewModelScope)
    private var positionJob: Job? = null

    /** Programmatic jump in progress: stale pager emissions are ignored. */
    private var pendingTarget: Pair<Int, Int>? = null

    /** In-memory chapter/verse count table (loaded once at startup). */
    private var verseCounts: Map<Long, Int> = emptyMap()

    var version by mutableStateOf(BibleVersionRegistry.default); private set
    var book by mutableStateOf(1); private set
    var chapter by mutableStateOf(1); private set
    var focusedVerse by mutableStateOf(0); private set
    var pulseVerse by mutableStateOf(0); private set

    /** Verse to scroll to WITHOUT highlighting (used after a version switch). */
    var anchorVerse by mutableStateOf(0); private set

    /** Verse currently at the top of the viewport of the current page. */
    private var topVerse = 0
    /**
     * Starts from the canonical table (no DB query) so the reader is usable
     * immediately; replaced by the version's real counts once loaded.
     */
    var chapterCounts by mutableStateOf(STANDARD_COUNTS); private set
    var currentContent by mutableStateOf<ChapterContent?>(null); private set
    var selectedVerses by mutableStateOf<Set<Int>>(emptySet()); private set
    var versionMeta by mutableStateOf<VersionMeta?>(null); private set
    var sheetVisible by mutableStateOf(false); private set
    var pendingImportId by mutableStateOf<String?>(null); private set

    /** True once the saved position is known (the first DataStore read). */
    var ready by mutableStateOf(false); private set

    /**
     * Bumped on every programmatic jump. The screen keys its PagerState on it,
     * so the pager is rebuilt directly AT the target page: no frame of the old
     * chapter and no scroll between the two.
     */
    var jumpId by mutableStateOf(0); private set

    val totalChapters: Int get() = chapterCounts.values.sum()

    init {
        viewModelScope.launch {
            val s = settingsRepo.initial()
            version = BibleVersionRegistry.byId(s.versionId)
            book = s.book.coerceIn(1, 66)
            chapter = s.chapter.coerceIn(1, chapterCounts[book] ?: 1)
            focusedVerse = s.verse
            pulseVerse = s.verse
            currentContent = chapterCache.peek(version.id, book, chapter)
            ready = true
            // Current chapter first (what the user sees), counts afterwards.
            loadCurrent()
            reloadCounts()
            verseCounts = repo.verseCounts(version.id)
            reloadMeta()
        }
        // Collected for the whole ViewModel lifetime (it survives while the
        // selection screen is on top), on Main.immediate: a request is applied
        // synchronously inside NavigationBus.open(), BEFORE popBackStack(), so
        // the reader's first frame after the pop is already the target.
        viewModelScope.launch {
            container.navigationBus.request.collect { request ->
                if (request == null) return@collect
                VersePerf.stage("reader received request")
                openAt(
                    VerseReference(
                        book = request.book,
                        chapter = request.chapter,
                        verse = request.verse.takeIf { it > 0 },
                    )
                )
                container.navigationBus.consume()
            }
        }
        viewModelScope.launch {
            container.importer.states.collect { states ->
                val id = pendingImportId ?: return@collect
                if (states[id]?.isDone == true) {
                    applyVersion(BibleVersionRegistry.byId(id))
                    pendingImportId = null
                    sheetVisible = false
                }
            }
        }
    }

    /** Opens the reader at a reference coming from the selection screen. */
    fun openAt(ref: VerseReference) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "openAt book=" + ref.book + " chapter=" + ref.chapter + " verse=" + (ref.verse ?: 0))
        }
        // Fully synchronous: every value below comes from memory.
        val b = ref.book.coerceIn(1, 66)
        val c = ref.chapter.coerceIn(1, chapterCounts[b] ?: 1)
        val cached = chapterCache.peek(version.id, b, c)
        val maxVerse = verseCounts[b * 1000L + c]
            ?: cached?.verses?.maxOfOrNull { it.verse }
            ?: Int.MAX_VALUE
        val v = (ref.verse ?: 0).coerceAtMost(maxVerse)
        pendingTarget = b to c
        book = b
        chapter = c
        selectedVerses = emptySet()
        focusedVerse = v
        pulseVerse = v
        anchorVerse = 0
        topVerse = v
        currentContent = cached
        jumpId++
        persistPosition()
        followJumpWithListen(b, c, v)
        if (cached == null) viewModelScope.launch { loadCurrent() } else prefetchSoon()
    }

    /** Peeks the shared cache (synchronous) so a cached page never starts blank. */
    fun peekChapter(v: String, b: Int, c: Int): ChapterContent? = chapterCache.peek(v, b, c)

    private fun prefetchSoon() {
        viewModelScope.launch { prefetchNeighbors() }
    }

    /** O(1) flat pager index from the precomputed cumulative table. */
    fun flatIndexOf(b: Int, c: Int): Int = BookCatalog.flatIndexOfStandard(b, c)

    /** Inverse of [flatIndexOf]; clamps to the last chapter when out of range. */
    fun fromFlatIndex(index: Int): Pair<Int, Int> {
        var remaining = index
        for (b in 1..66) {
            val count = chapterCounts[b] ?: continue
            if (remaining < count) return b to (remaining + 1)
            remaining -= count
        }
        val maxBook = chapterCounts.keys.maxOrNull() ?: 66
        return maxBook to (chapterCounts[maxBook] ?: 1)
    }

    /**
     * Pager page changed. While a programmatic jump is pending, stale page
     * emissions are ignored so they cannot overwrite the target position.
     */
    fun onPageChanged(page: Int) {
        val target = pendingTarget
        if (target != null) {
            if (flatIndexOf(target.first, target.second) == page) {
                pendingTarget = null
                VersePerf.stage("pager at target page")
            }
            return
        }
        val (b, c) = fromFlatIndex(page)
        onPositionChanged(b, c)
    }

    fun onPositionChanged(b: Int, c: Int) {
        if (b == book && c == chapter) return
        book = b; chapter = c
        selectedVerses = emptySet(); focusedVerse = 0; pulseVerse = 0
        anchorVerse = 0; topVerse = 0
        persistPosition()
        viewModelScope.launch { loadCurrent() }
    }

    fun tapVerse(verseNumber: Int) {
        focusedVerse = verseNumber
        selectedVerses = if (verseNumber in selectedVerses) selectedVerses - verseNumber
        else selectedVerses + verseNumber
        persistPosition()
    }

    fun clearSelection() { selectedVerses = emptySet() }
    fun consumePulse() { pulseVerse = 0 }
    fun consumeAnchor() { anchorVerse = 0 }

    /** Reported by the current page as it scrolls (plain field, no recomposition). */
    fun onTopVerse(verse: Int) { topVerse = verse }
    fun openVersionSheet() { sheetVisible = true }
    fun closeVersionSheet() { sheetVisible = false }

    fun isLastChapterOfBook(b: Int, c: Int): Boolean =
        chapterCounts.isNotEmpty() && chapterCounts[b] == c

    /** Cached chapter load (shared LRU); called from pager pages. */
    suspend fun loadChapter(v: String, b: Int, c: Int): ChapterContent? =
        chapterCache.get(v, b, c)

    fun selectVersion(v: BibleVersion) {
        if (v.id == version.id) { sheetVisible = false; return }
        viewModelScope.launch {
            if (settingsRepo.isImported(v.id)) {
                applyVersion(v); sheetVisible = false
            } else {
                pendingImportId = v.id
                container.importer.ensureImportedAsync(v)
            }
        }
    }

    /** Switches version keeping the position, clamped to the new version. */
    private suspend fun applyVersion(v: BibleVersion) {
        // The verse the user is looking at right now (top of the viewport),
        // falling back to the last tapped/jumped verse.
        val anchor = if (topVerse > 0) topVerse else focusedVerse
        // Load everything for the new version BEFORE touching UI state, so the
        // switch happens in one frame with the text already in memory.
        val counts = repo.chapterCounts(v.id)
        val newVerseCounts = repo.verseCounts(v.id)
        val b = book.coerceIn(1, 66)
        val c = chapter.coerceIn(1, counts[b] ?: 1)
        val content = runCatching { chapterCache.get(v.id, b, c) }.getOrNull()
        val maxVerse = newVerseCounts[b * 1000L + c]
            ?: content?.verses?.maxOfOrNull { it.verse } ?: 0
        val target = anchor.coerceAtMost(maxVerse)

        version = v
        container.tts.onVersionChanged(v) // voice follows the version
        BookNameLanguage.english = v.isEnglish // same frame as the text switch
        chapterCounts = counts
        verseCounts = newVerseCounts
        book = b
        chapter = c
        currentContent = content
        if (focusedVerse > maxVerse) focusedVerse = maxVerse
        anchorVerse = target
        topVerse = target
        pulseVerse = 0
        selectedVerses = emptySet()
        pendingTarget = book to chapter
        jumpId++
        settingsRepo.setVersionId(v.id)
        persistPosition()
        reloadMeta()
        loadCurrent()
    }

    /** "Listen": from the first selected verse, else the verse at the top of the screen. */
    fun startListening() {
        val start = selectedVerses.minOrNull() ?: topVerse.takeIf { it > 0 } ?: 1
        selectedVerses = emptySet()
        container.tts.play(version, book, chapter, start)
    }

    /** A jump while reading aloud continues reading at the new place (paused: stop). */
    private fun followJumpWithListen(b: Int, c: Int, verse: Int) {
        when (container.tts.state.value.status) {
            TtsStatus.PLAYING -> container.tts.play(version, b, c, verse.coerceAtLeast(1))
            TtsStatus.PAUSED -> container.tts.stop()
            TtsStatus.IDLE -> Unit
        }
    }

    /** Last verse number [verse] covers in the current chapter (combined verses: 1 -> 2). */
    fun verseEnd(verse: Int): Int = VerseSpans.end(verse, currentContent?.spanEnds.orEmpty())

    /** The selection plus the empty numbers merged into it ({1} -> {1, 2}). */
    private fun coveredSelection(): Set<Int> =
        VerseSpans.covered(selectedVerses, currentContent?.spanEnds.orEmpty())

    /** "Juan 3:16-18, 20" for the current selection (panel header, share text). */
    fun selectionLabel(): String =
        ReferenceFormatter.format(BookCatalog.book(book).displayName, chapter, coveredSelection())

    /** Share text in the form: "text" - Juan 3:16-17 (MBB12), or null. */
    fun selectedVersesText(): String? {
        val content = currentContent ?: return null
        if (selectedVerses.isEmpty()) return null
        val picked = content.verses.filter { it.verse in selectedVerses && !it.isEmpty }
        if (picked.isEmpty()) return null
        val text = picked.joinToString(" ") { it.text }
        val covered = VerseSpans.covered(picked.map { it.verse }, content.spanEnds)
        val ref = ReferenceFormatter.format(BookCatalog.book(book).displayName, chapter, covered)
        return "“" + text + "” - " + ref + " (" + version.shortName + ")"
    }

    /** Highlights every selected verse, then closes the panel. */
    fun highlightSelection(color: HighlightColor) {
        actions.highlight(book, chapter, coveredSelection(), color)
        clearSelection()
    }

    fun clearSelectionHighlight() {
        actions.clearHighlight(book, chapter, coveredSelection())
        clearSelection()
    }

    fun compareSelection() {
        actions.openCompare(selectionLabel(), book, chapter, coveredSelection())
    }

    /** Persists the position debounced so DataStore is not hit on every change. */
    private fun persistPosition() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            delay(POSITION_DEBOUNCE_MS)
            settingsRepo.setPosition(book, chapter, focusedVerse)
        }
    }

    private suspend fun reloadCounts() { chapterCounts = repo.chapterCounts(version.id) }
    private suspend fun reloadMeta() { versionMeta = repo.versionMeta(version.id) }

    private suspend fun loadCurrent() {
        currentContent = loadChapter(version.id, book, chapter)
        VersePerf.stage("chapter data ready")
        prefetchNeighbors()
    }

    /** Preloads the previous and next chapter so swiping never waits on Room. */
    private suspend fun prefetchNeighbors() {
        if (chapterCounts.isEmpty()) return
        val flat = flatIndexOf(book, chapter)
        if (flat > 0) {
            val (b, c) = fromFlatIndex(flat - 1)
            chapterCache.get(version.id, b, c)
        }
        if (flat + 1 < totalChapters) {
            val (b, c) = fromFlatIndex(flat + 1)
            chapterCache.get(version.id, b, c)
        }
    }

    companion object {
        private const val POSITION_DEBOUNCE_MS = 500L
        private val STANDARD_COUNTS: Map<Int, Int> =
            (1..66).associateWith { BookCatalog.standardChapterCounts[it - 1] }
    }
}
