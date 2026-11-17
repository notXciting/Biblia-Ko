package com.example.biblia.ui.selection

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.biblia.BuildConfig
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.ViewMode
import com.example.biblia.perf.VersePerf

private const val TAG = "VERSE_NAV"

/** Verses level: list rows or 5-column tiles; both taps publish + pop back. */
@Composable
fun VerseLevel(
    vm: SelectionViewModel,
    onVerseSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val book = vm.selectedBook
    val chapter = vm.selectedChapter
    var verses by remember(book, chapter) { mutableStateOf<List<Verse>?>(null) }
    LaunchedEffect(book, chapter) {
        verses = vm.versesFor(book, chapter)
    }
    val list = verses.orEmpty()
    val onPick: (Verse) -> Unit = { verse ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        VersePerf.markTap()
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Verse tapped: book=" + vm.selectedBook + " chapter=" + vm.selectedChapter + " verse=" + verse.verse)
        }
        vm.pickVerse(verse.verse, onVerseSelected)
    }
    if (vm.viewMode == ViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            gridItems(
                items = list,
                key = { it.verse },
                contentType = { "verseTile" },
            ) { verse ->
                VerseTile(
                    verse = verse,
                    isCurrent = book == vm.currentBook &&
                        chapter == vm.currentChapter &&
                        verse.verse == vm.currentVerse,
                    onClick = { onPick(verse) },
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(
                items = list,
                key = { it.verse },
                contentType = { "verseRow" },
            ) { verse ->
                VerseRow(
                    verse = verse,
                    isCurrent = book == vm.currentBook &&
                        chapter == vm.currentChapter &&
                        verse.verse == vm.currentVerse,
                    onClick = { onPick(verse) },
                )
            }
        }
    }
}
