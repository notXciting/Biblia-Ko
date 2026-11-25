@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.biblia.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.ReadingFont
import com.example.biblia.ui.KeepAppLocale
import com.example.biblia.ui.theme.ReadingFonts

/** Picker: every reading font, its name and a sample verse set in that font. */
@Composable
internal fun ReadingFontSheet(current: ReadingFont, sample: String, onSelect: (ReadingFont) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        KeepAppLocale {
            Column(Modifier.navigationBarsPadding()) {
                Text(
                    text = stringResource(R.string.settings_reading_font),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ReadingFont.entries) { font ->
                        FontOption(font, selected = font == current, sample = sample, onClick = { onSelect(font) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FontOption(font: ReadingFont, selected: Boolean, sample: String, onClick: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val usable = remember(font) { font == ReadingFont.SYSTEM || ReadingFonts.usable(context, font) }
    val family = remember(font, usable) { if (usable) ReadingFonts.family(font) else FontFamily.Serif }
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = usable,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colors.primaryContainer else colors.surfaceContainer,
        border = if (selected) BorderStroke(2.dp, colors.primary) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 14.dp)) {
            RadioButton(selected = selected, onClick = null, enabled = usable, modifier = Modifier.padding(12.dp))
            Column(Modifier.weight(1f).padding(top = 8.dp)) {
                Text(
                    text = readingFontName(font),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = family,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                )
                val tag = tagOf(font, usable)
                if (tag != null) {
                    Text(text = stringResource(tag), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                Text(
                    text = sample,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = family,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

private fun tagOf(font: ReadingFont, usable: Boolean): Int? = when {
    !usable -> R.string.reading_font_unavailable
    font == ReadingFont.DEFAULT -> R.string.reading_font_tag_default
    font == ReadingFont.ATKINSON -> R.string.reading_font_tag_legible
    font == ReadingFont.OPEN_DYSLEXIC -> R.string.reading_font_tag_dyslexia
    font == ReadingFont.SYSTEM -> R.string.reading_font_tag_system
    else -> null
}
