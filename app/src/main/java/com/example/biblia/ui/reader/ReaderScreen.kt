package com.example.biblia.ui.reader

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.biblia.R
import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.di.AppContainer
import com.example.biblia.domain.model.AppSettings
import com.example.biblia.domain.model.BookInfo
import com.example.biblia.domain.reference.ReferenceFormatter
import com.example.biblia.domain.tts.TtsController
import com.example.biblia.domain.tts.TtsState
import com.example.biblia.domain.tts.TtsStatus
import com.example.biblia.perf.VersePerf
import com.example.biblia.ui.listen.ListenBar
import kotlinx.coroutines.launch

/**
 * Reader: top bar + HorizontalPager over the flat chapter index. Navigation
 * requests are applied by [ReaderViewModel] itself (it collects the bus), and
 * the PagerState is keyed on [ReaderViewModel.jumpId] so a jump builds the
 * pager directly at the target page.
 */
@Composable
fun ReaderScreen(
    container: AppContainer,
    settings: AppSettings,
    onOpenSelection: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenNotes: () -> Unit = {},
    onOpenBibles: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val vm: ReaderViewModel = viewModel(
        factory = viewModelFactory { initializer { ReaderViewModel(container) } },
    )
    val importStates by container.importer.states.collectAsStateWithLifecycle()
    val tts by container.tts.state.collectAsStateWithLifecycle()
    val loadedVersions by container.versionLibrary.loaded.collectAsStateWithLifecycle(initialValue = emptySet())
    // Cross-references follow the selection; refreshed once the background import finishes.
    val crossRefsReady by container.crossRefImporter.ready.collectAsStateWithLifecycle()
    LaunchedEffect(vm.selectedVerses, vm.version.id, settings.showCrossRefs, crossRefsReady) {
        vm.loadCrossRefs(settings.showCrossRefs)
    }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ReaderTopBar(
            locationLabel = locationLabel(vm) { it.displayName },
            shortLocationLabel = locationLabel(vm) { it.displayShortName },
            versionShortName = vm.version.shortName,
            onSettingsClick = onOpenSettings,
            onLocationClick = onOpenSelection,
            onVersionClick = { vm.openVersionSheet() },
            barSize = settings.barSize,
        )
        if (vm.ready) {
            key(vm.jumpId) {
                val pagerState = rememberPagerState(
                    initialPage = vm.flatIndexOf(vm.book, vm.chapter),
                    pageCount = { vm.totalChapters },
                )
                ReaderBody(
                    vm, settings, pagerState, tts, container.tts,
                    onStudy = onOpenNotes,
                    onListen = { vm.startListening() },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    vm.actions.compare?.let { state ->
        CompareSheet(
            state = state,
            currentVersionId = vm.version.id,
            onDismiss = { vm.actions.closeCompare() },
            onAddVersion = {
                vm.actions.closeCompare()
                onOpenBibles()
            },
        )
    }
    vm.actions.noteEditor?.let { editor ->
        NoteSheet(
            state = editor,
            onSave = vm.actions.notes::save,
            onDelete = vm.actions.notes::delete,
            onDismiss = vm.actions.notes::dismiss,
            onCancel = vm.actions.notes::cancel,
            onAutosave = vm.actions.notes::autosave,
        )
    }
    if (vm.sheetVisible) {
        VersionBottomSheet(
            versions = BibleVersionRegistry.all.filter {
                it.id in loadedVersions || it.id == vm.version.id || importStates[it.id]?.isRunning == true
            },
            currentVersionId = vm.version.id,
            importStates = importStates,
            onSelect = { vm.selectVersion(it) },
            onDismiss = { vm.closeVersionSheet() },
            onManage = {
                vm.closeVersionSheet()
                onOpenBibles()
            },
        )
    }
}

@Composable
private fun ReaderBody(
    vm: ReaderViewModel,
    settings: AppSettings,
    pagerState: PagerState,
    tts: TtsState,
    listen: TtsController,
    onStudy: () -> Unit,
    onListen: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // Back first dismisses the verse panel instead of leaving the app.
    BackHandler(enabled = vm.selectedVerses.isNotEmpty()) { vm.clearSelection() }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page -> vm.onPageChanged(page) }
    }
    // Pager follows VM position changes that are not jumps (e.g. clamping).
    LaunchedEffect(vm.book, vm.chapter) {
        val target = vm.flatIndexOf(vm.book, vm.chapter)
        if (pagerState.currentPage != target) {
            VersePerf.stage("pager jump started")
            pagerState.scrollToPage(target)
        }
    }

    // "Listen" rolled into another chapter: follow it. Remembered per pager
    // build, so a jump (new pager) never pulls the reader back.
    val followed = remember { mutableStateOf(tts.book to tts.chapter) }
    LaunchedEffect(tts.book, tts.chapter) {
        val pos = tts.book to tts.chapter
        if (!tts.isActive || pos == followed.value) return@LaunchedEffect
        followed.value = pos
        val target = vm.flatIndexOf(tts.book, tts.chapter)
        if (pagerState.currentPage != target) pagerState.animateScrollToPage(target)
    }
    val listening = tts.isActive && tts.versionId == vm.version.id
    // Where reading aloud is; when it pauses or stops (stop clears TtsState),
    // the reader's title and position move to that verse.
    val lastSpoken = remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    LaunchedEffect(tts.status, tts.book, tts.chapter, tts.verse) {
        val here = Triple(tts.book, tts.chapter, tts.verse).takeIf { tts.verse > 0 && tts.versionId == vm.version.id }
        when (tts.status) {
            TtsStatus.PLAYING -> if (here != null) lastSpoken.value = here
            // Paused (prev/next while paused move it too): follow the paused verse.
            TtsStatus.PAUSED -> here?.let { (b, c, v) -> lastSpoken.value = here; vm.syncToListen(b, c, v) }
            TtsStatus.IDLE -> {
                lastSpoken.value?.let { (b, c, v) -> vm.syncToListen(b, c, v) }
                lastSpoken.value = null
            }
        }
    }
    // Bottom bar: hides while reading downward, back on scroll up or a new chapter.
    val (hideOnScroll, barShown) = rememberHideOnScroll()
    LaunchedEffect(pagerState.currentPage) { barShown.value = true }

    Box(modifier.fillMaxSize().nestedScroll(hideOnScroll)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { it },
            beyondViewportPageCount = 1,
        ) { page ->
            val (b, c) = vm.fromFlatIndex(page)
            ChapterPage(
                versionId = vm.version.id,
                book = b,
                chapter = c,
                fontSizeSp = settings.fontSizeSp,
                lineSpacingMultiplier = settings.lineSpacing.multiplier,
                verseLayout = settings.verseLayout,
                isCurrentPage = page == pagerState.currentPage,
                vm = vm,
                speakingVerse = if (listening && tts.book == b && tts.chapter == c) tts.verse else 0,
            )
        }
        // The verse panel and the Listen player take the bar's place.
        ReaderBottomBar(
            visible = barShown.value && vm.selectedVerses.isEmpty() && !listening,
            canGoBack = pagerState.currentPage > 0,
            canGoForward = pagerState.currentPage < vm.totalChapters - 1,
            onPrevious = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
            onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
            onStudy = onStudy,
            onListen = onListen,
            modifier = Modifier.align(Alignment.BottomCenter),
            barSize = settings.barSize,
        )
        ListenBar(
            visible = listening && vm.selectedVerses.isEmpty(),
            reference = if (tts.verse > 0) {
                ReferenceFormatter.format(BookCatalog.book(tts.book).displayName, tts.chapter, (tts.verse..tts.verseEnd).toList())
            } else "",
            shortReference = if (tts.verse > 0) {
                ReferenceFormatter.format(BookCatalog.book(tts.book).displayShortName, tts.chapter, (tts.verse..tts.verseEnd).toList())
            } else "",
            playing = tts.status == TtsStatus.PLAYING,
            onPlayPause = { if (tts.status == TtsStatus.PLAYING) listen.pause() else listen.resume() },
            onPrevious = { listen.previous() },
            onNext = { listen.next() },
            onStop = { listen.stop() },
            barSize = settings.barSize,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
        )
        val selected = vm.selectedVerses
        val chapterHighlights = remember(vm.book, vm.chapter) { vm.actions.highlights(vm.book, vm.chapter) }
            .collectAsState(initial = emptyMap()).value
        val selectedColors = selected.map { chapterHighlights[it] }
        VerseActionBar(
            selectedCount = selected.size,
            reference = if (selected.isEmpty()) "" else vm.selectionLabel(),
            // Checkmark only when EVERY selected verse already has that color.
            activeColor = selectedColors.firstOrNull()?.takeIf { c -> selectedColors.all { it == c } },
            anyHighlighted = selectedColors.any { it != null },
            onHighlight = { vm.highlightSelection(it) },
            onRemoveHighlight = { vm.clearSelectionHighlight() },
            onCompare = { vm.compareSelection() },
            onNote = { vm.noteSelection() },
            footer = {
                if (settings.showCrossRefs) {
                    CrossRefSection(vm.crossRefs, settings.crossRefFont, onOpen = { vm.openCrossRef(it) })
                }
            },
            onCopy = {
                vm.selectedVersesText()?.let { text ->
                    clipboard.setText(AnnotatedString(text))
                    Toast.makeText(context, R.string.reader_copied_toast, Toast.LENGTH_SHORT).show()
                }
            },
            onShare = {
                vm.selectedVersesText()?.let { text ->
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(send, null))
                }
            },
            onClose = { vm.clearSelection() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp),
        )
    }
}

@Composable
private fun locationLabel(vm: ReaderViewModel, bookName: (BookInfo) -> String): String {
    val name = bookName(BookCatalog.book(vm.book))
    val end = vm.verseEnd(vm.focusedVerse)
    return if (vm.focusedVerse > 0 && end > vm.focusedVerse) {
        ReferenceFormatter.format(name, vm.chapter, (vm.focusedVerse..end).toList()) // combined "1-2"
    } else if (vm.focusedVerse > 0) {
        stringResource(R.string.reader_verse_reference, name, vm.chapter, vm.focusedVerse)
    } else {
        stringResource(R.string.reader_chapter_heading, name, vm.chapter)
    }
}
