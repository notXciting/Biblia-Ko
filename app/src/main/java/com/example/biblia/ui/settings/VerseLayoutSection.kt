package com.example.biblia.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biblia.R
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseLayout
import com.example.biblia.ui.reader.ChapterFlowText

/**
 * Verse layout: Paragraph / Verse by verse, with a LIVE preview rendered by
 * the reader's own ChapterFlowText (same font size, spacing, accent), so what
 * you see here is exactly what the reader shows.
 */
@Composable
fun VerseLayoutSection(
    current: VerseLayout,
    previewVerses: List<Verse>,
    previewReference: String,
    fontSizeSp: Float,
    lineSpacingMultiplier: Float,
    onSelect: (VerseLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
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
        if (previewVerses.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_font_preview) + " · " + previewReference,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    AnimatedContent(
                        targetState = current,
                        transitionSpec = {
                            (fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(120)))
                                .using(SizeTransform(clip = false))
                        },
                        label = "verseLayoutPreview",
                    ) { layout ->
                        ChapterFlowText(
                            verses = previewVerses,
                            fontSize = fontSizeSp.sp,
                            lineHeightMultiplier = lineSpacingMultiplier,
                            accent = MaterialTheme.colorScheme.primary,
                            layout = layout,
                            selectedVerses = emptySet(),
                            pulseVerse = 0,
                            onVerseTap = {},
                            onPulseFinished = {},
                        )
                    }
                }
            }
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
