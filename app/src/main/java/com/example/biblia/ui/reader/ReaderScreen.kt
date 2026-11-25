package com.example.biblia.ui.reader

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
    modifier: Modifier = Modifier,
) {
    val vm: ReaderViewModel = viewModel(
        factory = viewModelFactory { initializer { ReaderViewModel(container) } },
    )
    val importStates by container.importer.states.collectAsStateWithLifecycle()
    val tts by container.tts.state.collectAsStateWithLifecycle()
    // Cross-references follow the selection; refreshed once the background import finishes.
    val crossRefsReady by container.crossRefImporter.ready.collectAsStateWithLifecycle()
    LaunchedEffect(vm.selectedVerses, vm.version.id, settings.showCrossRefs, crossRefsReady) {
        vm.loadCrossRefs(settings.showCrossRefs)
    }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ReaderTopBar(
            locationLabel = locationLabel(vm),
            versionShortName = vm.version.shortName,
            onSettingsClick = onOpenSettings,
            onLocationClick = onOpenSelection,
            onVersionClick = { vm.openVersionSheet() },
            listening = tts.isActive,
            onNotesClick = onOpenNotes,
            onListenClick = { if (tts.isActive) container.tts.stop() else vm.startListening() },
        )
        if (vm.ready) {
            key(vm.jumpId) {
                val pagerState = rememberPagerState(
                    initialPage = vm.flatIndexOf(vm.book, vm.chapter),
                    pageCount = { vm.totalChapters },
                )
                ReaderBody(vm, settings, pagerState, tts, container.tts, Modifier.weight(1f))
            }
        }
    }
    vm.actions.compare?.let { state ->
        CompareSheet(state = state, currentVersionId = vm.version.id, onDismiss = { vm.actions.closeCompare() })
    }
    vm.actions.noteEditor?.let { editor ->
        NoteSheet(
            state = editor,
            onSave = { vm.actions.saveNote(it) },
            onDelete = { vm.actions.deleteNote() },
            onDismiss = { vm.actions.closeNote() },
        )
    }
    if (vm.sheetVisible) {
        VersionBottomSheet(
            versions = BibleVersionRegistry.all,
            currentVersionId = vm.version.id,
            importStates = importStates,
            onSelect = { vm.selectVersion(it) },
            onDismiss = { vm.closeVersionSheet() },
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

    Box(modifier.fillMaxSize()) {
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
        val showArrows = vm.selectedVerses.isEmpty() && !listening
        PageArrow(
            visible = showArrows && pagerState.currentPage > 0,
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            description = stringResource(R.string.action_previous_chapter),
            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
            modifier = Modifier.align(Alignment.BottomStart),
        )
        PageArrow(
            visible = showArrows && pagerState.currentPage < vm.totalChapters - 1,
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            description = stringResource(R.string.action_next_chapter),
            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
            modifier = Modifier.align(Alignment.BottomEnd),
        )
        ListenBar(
            visible = listening && vm.selectedVerses.isEmpty(),
            reference = if (tts.verse > 0) {
                ReferenceFormatter.format(BookCatalog.book(tts.book).displayName, tts.chapter, (tts.verse..tts.verseEnd).toList())
            } else "",
            playing = tts.status == TtsStatus.PLAYING,
            onPlayPause = { if (tts.status == TtsStatus.PLAYING) listen.pause() else listen.resume() },
            onPrevious = { listen.previous() },
            onNext = { listen.next() },
            onStop = { listen.stop() },
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

/** Small, quiet circular arrow floating at a bottom corner. */
@Composable
private fun PageArrow(
    visible: Boolean,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shadowElevation = 3.dp,
            modifier = Modifier.size(44.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = description)
            }
        }
    }
}

@Composable
private fun locationLabel(vm: ReaderViewModel): String {
    val name = BookCatalog.book(vm.book).displayName
    val end = vm.verseEnd(vm.focusedVerse)
    return if (vm.focusedVerse > 0 && end > vm.focusedVerse) {
        ReferenceFormatter.format(name, vm.chapter, (vm.focusedVerse..end).toList()) // combined "1-2"
    } else if (vm.focusedVerse > 0) {
        stringResource(R.string.reader_verse_reference, name, vm.chapter, vm.focusedVerse)
    } else {
        stringResource(R.string.reader_chapter_heading, name, vm.chapter)
    }
}
