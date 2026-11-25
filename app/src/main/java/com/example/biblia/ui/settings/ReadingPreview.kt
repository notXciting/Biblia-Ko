package com.example.biblia.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biblia.R
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseLayout
import com.example.biblia.ui.reader.ChapterFlowText
import com.example.biblia.ui.theme.LocalPageStyle
import com.example.biblia.ui.theme.LocalReadingType

/**
 * One live preview for the whole Reading page, pinned above the settings:
 * the reader's own ChapterFlowText with the current font, size (live while
 * dragging), spacing and layout. Falls back to a fixed verse until loaded.
 */
@Composable
internal fun ReadingPreview(
    verses: List<Verse>,
    reference: String,
    fontSizeSp: Float,
    lineSpacingMultiplier: Float,
    layout: VerseLayout,
    modifier: Modifier = Modifier,
) {
    val page = LocalPageStyle.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = page?.let { MaterialTheme.colorScheme.background } ?: MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_font_preview) +
                    " · " + reference.ifEmpty { stringResource(R.string.font_preview_reference) },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            // Capped so a 32sp preview can't push the settings off screen.
            Column(Modifier.heightIn(max = 180.dp).clipToBounds().padding(top = 6.dp)) {
                if (verses.isEmpty()) {
                    Text(
                        text = stringResource(R.string.font_preview_text),
                        fontSize = fontSizeSp.sp,
                        fontFamily = LocalReadingType.current.family,
                        lineHeight = (fontSizeSp * lineSpacingMultiplier * LocalReadingType.current.lineHeight).sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                } else {
                    ChapterFlowText(
                        verses = verses,
                        fontSize = fontSizeSp.sp,
                        lineHeightMultiplier = lineSpacingMultiplier,
                        accent = MaterialTheme.colorScheme.primary,
                        numberColor = page?.verseNumber ?: MaterialTheme.colorScheme.primary,
                        balanced = page != null,
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
