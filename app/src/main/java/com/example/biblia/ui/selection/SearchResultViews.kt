package com.example.biblia.ui.selection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseReference
import com.example.biblia.domain.search.SearchQueryBuilder
import com.example.biblia.domain.search.SnippetBuilder
import com.example.biblia.domain.search.VerseSnippet

/** Shared search views: smart-jump card + result card with highlighted snippet. */

@Composable
internal fun ReferenceJumpCard(ref: VerseReference, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.search_jump_to, label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
internal fun SearchResultCard(verse: Verse, query: String, onClick: () -> Unit) {
    val tokens = remember(verse, query) { SearchQueryBuilder.foldTokens(query) }
    val snippet = remember(verse, tokens) { SnippetBuilder.build(verse.text, tokens) }
    val reference = BookCatalog.book(verse.book).displayName + " " + verse.chapter + ":" + verse.verse
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = reference,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = highlightSnippet(snippet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun highlightSnippet(snippet: VerseSnippet): AnnotatedString =
    buildAnnotatedString {
        append(snippet.text)
        snippet.highlightRanges.forEach { range ->
            addStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                ),
                range.first,
                range.last + 1,
            )
        }
    }
