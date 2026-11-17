package com.example.biblia.ui.selection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.SearchScope

/** Search scope selector: Lahat / Mga Aklat / Teksto (remembered in DataStore). */
@Composable
fun ScopeChips(
    current: SearchScope,
    onSelect: (SearchScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScopeChip(
            value = SearchScope.ALL,
            label = stringResource(R.string.filter_all),
            current = current,
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
        )
        ScopeChip(
            value = SearchScope.BOOKS,
            label = stringResource(R.string.selection_breadcrumb_books),
            current = current,
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
        )
        ScopeChip(
            value = SearchScope.TEXT,
            label = stringResource(R.string.scope_text),
            current = current,
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ScopeChip(
    value: SearchScope,
    label: String,
    current: SearchScope,
    onSelect: (SearchScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = current == value,
        onClick = { onSelect(value) },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
    )
}
