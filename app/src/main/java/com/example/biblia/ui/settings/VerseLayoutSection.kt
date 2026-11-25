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
import com.example.biblia.domain.model.VerseLayout

/** Verse layout: Paragraph / Verse by verse (shown live in the page's [ReadingPreview]). */
@Composable
fun VerseLayoutSection(current: VerseLayout, onSelect: (VerseLayout) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.settings_verse_layout))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LayoutChip(VerseLayout.PARAGRAPH, stringResource(R.string.verse_layout_paragraph), current, onSelect, Modifier.weight(1f))
            LayoutChip(VerseLayout.VERSE_BY_VERSE, stringResource(R.string.verse_layout_separate), current, onSelect, Modifier.weight(1f))
        }
    }
}

@Composable
private fun LayoutChip(
    value: VerseLayout,
    label: String,
    current: VerseLayout,
    onSelect: (VerseLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = current == value,
        onClick = { onSelect(value) },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
    )
}
