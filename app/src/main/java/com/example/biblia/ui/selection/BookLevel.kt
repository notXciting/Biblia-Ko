@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.biblia.ui.selection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.BookInfo
import com.example.biblia.domain.model.Testament
import com.example.biblia.domain.model.ViewMode

/** Books level: list rows or card grid; sticky testament headers when unfiltered. */
@Composable
fun BookLevel(vm: SelectionViewModel, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val books = remember(vm.filter) { vm.filteredBooks() }
    val onPick: (BookInfo) -> Unit = { book ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        vm.pickBook(book.number)
    }
    if (vm.viewMode == ViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            // Same testament headers as the list view, as full-width rows.
            val groups = if (spansBothTestaments(books)) {
                listOf(
                    R.string.filter_old_testament to books.filter { it.testament == Testament.OLD },
                    R.string.filter_new_testament to books.filter { it.testament == Testament.NEW },
                )
            } else {
                listOf(null to books)
            }
            groups.forEach { (titleRes, groupBooks) ->
                if (titleRes != null) {
                    item(key = "header-$titleRes", span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                        SectionHeader(stringResource(titleRes), horizontalPadding = 4.dp)
                    }
                }
                gridItems(
                    items = groupBooks,
                    key = { it.number },
                    contentType = { "bookCard" },
                ) { book ->
                    BookCard(
                        book = book,
                        isCurrent = book.number == vm.currentBook,
                        chapterCount = vm.chapterCountOf(book.number),
                        onClick = { onPick(book) },
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // Testament headers whenever the list spans both testaments
            // (All, and cross-testament themes like Narrative or Prophecy).
            if (spansBothTestaments(books)) {
                stickyHeader(key = "ot") {
                    SectionHeader(stringResource(R.string.filter_old_testament))
                }
                items(
                    items = books.filter { it.testament == Testament.OLD },
                    key = { it.number },
                    contentType = { "bookRow" },
                ) { book ->
                    BookRow(
                        book = book,
                        isCurrent = book.number == vm.currentBook,
                        chapterCount = vm.chapterCountOf(book.number),
                        onClick = { onPick(book) },
                    )
                }
                stickyHeader(key = "nt") {
                    SectionHeader(stringResource(R.string.filter_new_testament))
                }
                items(
                    items = books.filter { it.testament == Testament.NEW },
                    key = { it.number },
                    contentType = { "bookRow" },
                ) { book ->
                    BookRow(
                        book = book,
                        isCurrent = book.number == vm.currentBook,
                        chapterCount = vm.chapterCountOf(book.number),
                        onClick = { onPick(book) },
                    )
                }
            } else {
                items(
                    items = books,
                    key = { it.number },
                    contentType = { "bookRow" },
                ) { book ->
                    BookRow(
                        book = book,
                        isCurrent = book.number == vm.currentBook,
                        chapterCount = vm.chapterCountOf(book.number),
                        onClick = { onPick(book) },
                    )
                }
            }
        }
    }
}

/** Testament headers are shown when the listed books span both testaments. */
private fun spansBothTestaments(books: List<BookInfo>): Boolean =
    books.any { it.testament == Testament.OLD } && books.any { it.testament == Testament.NEW }
