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
import com.example.biblia.domain.model.AppLanguage

/**
 * App language: follow the Bible version / Tagalog / English. Language names
 * are always written in their own language (convention for language pickers).
 */
@Composable
fun LanguageSection(
    current: AppLanguage,
    versionLanguage: String,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.settings_language))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LanguageChip(AppLanguage.FOLLOW_VERSION, stringResource(R.string.language_follow_version), current, onSelect, Modifier.weight(1.4f))
            LanguageChip(AppLanguage.TAGALOG, "Tagalog", current, onSelect, Modifier.weight(1f))
            LanguageChip(AppLanguage.ENGLISH, "English", current, onSelect, Modifier.weight(1f))
        }
        if (current == AppLanguage.FOLLOW_VERSION) {
            Text(
                text = stringResource(R.string.language_follow_hint, versionLanguage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun LanguageChip(
    value: AppLanguage,
    label: String,
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = current == value,
        onClick = { onSelect(value) },
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
    )
}
