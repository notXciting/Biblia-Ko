package com.example.biblia.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.example.biblia.domain.model.LineSpacing

/** Line spacing: Compact / Normal / Relaxed chips, applied live via DataStore. */
@Composable
fun LineSpacingSection(
    current: LineSpacing,
    onSelect: (LineSpacing) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.settings_line_spacing))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SpacingChip(
                value = LineSpacing.COMPACT,
                label = stringResource(R.string.spacing_compact),
                current = current,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
            SpacingChip(
                value = LineSpacing.NORMAL,
                label = stringResource(R.string.spacing_normal),
                current = current,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
            SpacingChip(
                value = LineSpacing.RELAXED,
                label = stringResource(R.string.spacing_relaxed),
                current = current,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SpacingChip(
    value: LineSpacing,
    label: String,
    current: LineSpacing,
    onSelect: (LineSpacing) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = current == value,
        onClick = { onSelect(value) },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
    )
}
