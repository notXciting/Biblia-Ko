package com.example.biblia.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.CrossRefFont

/** "Mga kaugnay na talata": show them in the verse panel, and their font. */
@Composable
fun CrossRefSettingsSection(
    show: Boolean,
    font: CrossRefFont,
    onShow: (Boolean) -> Unit,
    onFont: (CrossRefFont) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.settings_crossrefs))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_crossrefs_show), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.settings_crossrefs_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = show, onCheckedChange = onShow)
        }
        if (show) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                FilterChip(
                    selected = font == CrossRefFont.SERIF_ITALIC,
                    onClick = { onFont(CrossRefFont.SERIF_ITALIC) },
                    label = { Text(stringResource(R.string.crossref_font_serif), fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic) },
                )
                FilterChip(
                    selected = font == CrossRefFont.SANS,
                    onClick = { onFont(CrossRefFont.SANS) },
                    label = { Text(stringResource(R.string.crossref_font_sans), fontFamily = FontFamily.SansSerif) },
                )
            }
        }
    }
}
