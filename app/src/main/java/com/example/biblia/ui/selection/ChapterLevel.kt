package com.example.biblia.ui.selection

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.biblia.BuildConfig
import com.example.biblia.R
import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.domain.model.ViewMode

private const val TAG = "VERSE_NAV"

/** Chapters level: "Buksan ang kabanata" button + list rows or 5-column tiles. */
@Composable
fun ChapterLevel(
    vm: SelectionViewModel,
    onChapterSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val book = vm.selectedBook
    val count = vm.chapterCountOf(book).coerceAtLeast(1)
    val bookName = BookCatalog.book(book).displayName
    val chapters = (1..count).toList()
    val onPick: (Int) -> Unit = { n ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        vm.pickChapter(n)
    }
    val onOpen: (Int) -> Unit = { n ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Chapter open tapped: book=" + book + " chapter=" + n)
        }
        vm.openChapter(n, onChapterSelected)
    }
    Column(modifier.fillMaxSize()) {
        FilledTonalButton(
            onClick = { onOpen(vm.selectedChapter) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.action_open_chapter) + " " +
                    bookName + " " + vm.selectedChapter
            )
        }
        if (vm.viewMode == ViewMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                gridItems(
                    items = chapters,
                    key = { it },
                    contentType = { "chapterTile" },
                ) { n ->
                    NumberTile(
                        number = n,
                        isCurrent = book == vm.currentBook && n == vm.currentChapter,
                        onClick = { onPick(n) },
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(
                    items = chapters,
                    key = { it },
                    contentType = { "chapterRow" },
                ) { n ->
                    ChapterRow(
                        chapter = n,
                        isCurrent = book == vm.currentBook && n == vm.currentChapter,
                        onClick = { onPick(n) },
                        onOpenDirect = { onOpen(n) },
                    )
                }
            }
        }
    }
}
