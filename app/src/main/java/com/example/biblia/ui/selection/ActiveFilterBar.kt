package com.example.biblia.ui.selection

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.BookFilter

/** "Prison Letters · 4 books  ✕" — shows the active filter; tap to clear it. */
@Composable
fun ActiveFilterBar(filter: BookFilter, onClear: () -> Unit, modifier: Modifier = Modifier) {
    InputChip(
        selected = true,
        onClick = onClear,
        label = {
            Text(
                stringResource(filterLabel(filter)) + " · " +
                    stringResource(R.string.filter_book_count, filter.bookCount)
            )
        },
        leadingIcon = {
            Icon(Icons.Filled.FilterList, contentDescription = null, Modifier.size(InputChipDefaults.IconSize))
        },
        trailingIcon = {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.filter_clear),
                Modifier.size(InputChipDefaults.IconSize),
            )
        },
        modifier = modifier.padding(start = 16.dp, bottom = 4.dp),
    )
}
