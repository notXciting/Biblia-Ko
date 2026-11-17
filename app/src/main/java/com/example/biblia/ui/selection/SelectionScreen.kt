package com.example.biblia.ui.selection

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.di.AppContainer
import com.example.biblia.domain.model.AppSettings
import com.example.biblia.domain.model.BookFilter
import com.example.biblia.domain.model.BookInfo

/**
 * Verse selection: top row (filter/search/view) + scope chips + breadcrumb +
 * drill-down (Books -> Chapters -> Verses) or scoped search results. Verse and
 * chapter picks publish an OpenVerseRequest and pop back to the reader.
 */
@Composable
fun SelectionScreen(
    container: AppContainer,
    settings: AppSettings,
    onBack: () -> Unit,
    onVerseSelected: () -> Unit = {},
    onChapterSelected: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val vm: SelectionViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SelectionViewModel(container, settings) }
        },
    )
    var filterSheetVisible by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val haptics = LocalHapticFeedback.current
    val keyboard = LocalSoftwareKeyboardController.current

    BackHandler {
        when {
            vm.searchActive -> vm.toggleSearch(false)
            vm.goUp() -> Unit
            else -> onBack()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding(),
    ) {
        SelectionTopRow(
            filter = vm.filter,
            searchActive = vm.searchActive,
            query = vm.query,
            viewMode = vm.viewMode,
            onFilterClick = { filterSheetVisible = true },
            onSearchToggle = { active -> vm.toggleSearch(active) },
            onQueryChange = vm::updateQuery,
            onViewToggle = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.toggleViewMode()
            },
            onBack = {
                when {
                    vm.searchActive -> vm.toggleSearch(false)
                    vm.goUp() -> Unit
                    else -> onBack()
                }
            },
            focusRequester = focusRequester,
        )
        if (vm.searchActive) {
            ScopeChips(
                current = vm.searchScope,
                onSelect = { value ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.selectScope(value)
                },
            )
        }
        AnimatedVisibility(visible = !vm.searchActive) {
            Breadcrumb(
                level = vm.level,
                bookName = BookCatalog.book(vm.selectedBook).displayName,
                chapter = vm.selectedChapter,
                onBooksClick = { vm.goBooks() },
                onChaptersClick = { vm.goChapters() },
            )
        }
        AnimatedVisibility(visible = vm.filter != BookFilter.ALL) {
            ActiveFilterBar(filter = vm.filter, onClear = { vm.selectFilter(BookFilter.ALL) })
        }
        val pane = if (vm.searchActive && vm.query.isNotBlank()) null else vm.level
        AnimatedContent(
            targetState = pane,
            transitionSpec = { paneTransition(initialState, targetState) },
            label = "selectionPane",
            modifier = Modifier.weight(1f),
        ) { target ->
            Box(Modifier.fillMaxSize()) {
                when (target) {
                    null -> {
                        val onBookSelected: (BookInfo) -> Unit = { book ->
                            keyboard?.hide()
                            vm.toggleSearch(false)
                            vm.pickBook(book.number)
                        }
                        SearchResults(
                            vm = vm,
                            onVerseSelected = onVerseSelected,
                            onBookSelected = onBookSelected,
                        )
                    }
                    SelectionLevel.BOOKS -> BookLevel(vm = vm, Modifier.fillMaxSize())
                    SelectionLevel.CHAPTERS ->
                        ChapterLevel(vm = vm, onChapterSelected = onChapterSelected, Modifier.fillMaxSize())
                    SelectionLevel.VERSES ->
                        VerseLevel(vm = vm, onVerseSelected = onVerseSelected, Modifier.fillMaxSize())
                }
            }
        }
    }
    if (filterSheetVisible) {
        FilterSheet(
            current = vm.filter,
            onSelect = { value ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.selectFilter(value)
                filterSheetVisible = false
            },
            onDismiss = { filterSheetVisible = false },
        )
    }
}

/**
 * Drilling deeper slides in from the right, going up slides back from the
 * left; entering/leaving search results is a quick crossfade.
 */
private fun paneTransition(from: SelectionLevel?, to: SelectionLevel?): ContentTransform {
    if (from == null || to == null) {
        return fadeIn(tween(160)) togetherWith fadeOut(tween(100))
    }
    val forward = to.ordinal > from.ordinal
    val sign = if (forward) 1 else -1
    val enter = slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { sign * it / 5 } +
        fadeIn(tween(200, delayMillis = 40))
    val exit = slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { -sign * it / 5 } +
        fadeOut(tween(120))
    return enter togetherWith exit
}
