package com.example.biblia.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.BarSize

/** "Laki ng mga bar": the reader's top and bottom bars, Siksik / Normal / Malaki. */
@Composable
fun BarSizeSection(current: BarSize, onSelect: (BarSize) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.settings_bar_size))
        Text(
            text = stringResource(R.string.settings_bar_size_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            listOf(
                BarSize.COMPACT to R.string.bar_size_compact,
                BarSize.NORMAL to R.string.bar_size_normal,
                BarSize.LARGE to R.string.bar_size_large,
            ).forEach { (size, label) ->
                FilterChip(
                    selected = current == size,
                    onClick = { onSelect(size) },
                    label = { Text(stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
