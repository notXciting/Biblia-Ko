package com.example.biblia.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.ReadingFont
import com.example.biblia.ui.theme.LocalReadingType

/**
 * "Font ng pagbasa": the reading typeface in use, shown in that face. Tapping
 * opens [ReadingFontSheet]. [sample] is a verse from the Bible in use.
 */
@Composable
fun ReadingFontSection(current: ReadingFont, sample: String?, onSelect: (ReadingFont) -> Unit, modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    // What is actually drawn: a font that failed to load shows as the system font.
    val inUse = LocalReadingType.current
    Column(modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.settings_reading_font))
        Surface(
            onClick = { open = true },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = readingFontName(inUse.font),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = inUse.family,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.reading_font_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    if (open) {
        ReadingFontSheet(
            current = current,
            sample = sample ?: stringResource(R.string.font_preview_text),
            onSelect = { font ->
                onSelect(font)
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

/** Font names are proper nouns; only the system option is translated. */
@Composable
internal fun readingFontName(font: ReadingFont): String =
    if (font == ReadingFont.SYSTEM) stringResource(R.string.reading_font_system) else font.familyName
