package com.example.biblia.ui.selection

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.biblia.BuildConfig
import com.example.biblia.R
import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.domain.model.BookInfo
import com.example.biblia.domain.model.SearchScope
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseReference

private const val TAG = "VERSE_NAV"

/**
 * Search results, scope-aware:
 *  - Lahat: reference smart-jump (top), up to 5 matching books, then FTS text.
 *  - Mga Aklat: reference smart-jump + full book list (no FTS).
 *  - Teksto: FTS text results only.
 */
@Composable
fun SearchResults(
    vm: SelectionViewModel,
    onVerseSelected: () -> Unit,
    onBookSelected: (BookInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        val ref = vm.reference
        if (ref != null && vm.searchScope != SearchScope.TEXT) {
            val onOpenReference = remember(ref) {
                {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Reference jump tapped: " + referenceLabel(ref))
                    vm.openReference(ref, onVerseSelected)
                }
            }
            ReferenceJumpCard(ref = ref, label = referenceLabel(ref), onClick = onOpenReference)
        }
        if (vm.searching && vm.searchScope != SearchScope.BOOKS) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (vm.searchScope == SearchScope.BOOKS) {
            BookResults(
                books = vm.bookResults,
                currentBook = vm.currentBook,
                chapterCountOf = vm::chapterCountOf,
                onBookSelected = onBookSelected,
                modifier = Modifier.weight(1f),
            )
            return@Column
        }
        if (vm.searchScope == SearchScope.ALL) {
            // In "Lahat" the book section is a suggestion: hide it when the
            // query is a reference or nothing matched (no empty-state noise).
            if (ref == null && vm.bookResults.isNotEmpty()) BookResults(
                books = vm.bookResults,
                currentBook = vm.currentBook,
                chapterCountOf = vm::chapterCountOf,
                onBookSelected = onBookSelected,
            )
        } else if (!vm.searching) { // no misleading "0 results" while searching
            Text(
                text = stringResource(R.string.search_result_count, vm.results.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            if (vm.truncated) {
                Text(
                    text = stringResource(R.string.search_truncated),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        if (vm.reference != null) return@Column
        if (!vm.searching && vm.results.isEmpty()) {
            EmptySearchState(Modifier.weight(1f))
        } else {
            val onOpenVerse: (Verse) -> Unit = { verse ->
                vm.pickSearchResult(verse, onVerseSelected)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(
                    items = vm.results,
                    key = { "" + it.book + ":" + it.chapter + ":" + it.verse },
                    contentType = { "searchResult" },
                ) { verse ->
                    SearchResultCard(
                        verse = verse,
                        query = vm.query,
                        onClick = { onOpenVerse(verse) },
                    )
                }
            }
        }
    }
}

private fun referenceLabel(ref: VerseReference): String {
    val name = BookCatalog.book(ref.book).displayName
    return if (ref.verse != null) name + " " + ref.chapter + ":" + ref.verse
    else name + " " + ref.chapter
}

@Composable
private fun EmptySearchState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.search_no_results),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.search_no_results_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
