package com.example.biblia.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.tts.TtsVoice
import com.example.biblia.domain.tts.VoiceStatus
import com.example.biblia.ui.KeepAppLocale
import com.example.biblia.ui.listen.languageLabel

/**
 * One language's voice: tap to pick from the phone's OFFLINE voices (or the
 * best one automatically). Disabled until that language is installed offline.
 */
@Composable
internal fun VoicePickerRow(
    language: String,
    selected: String?,
    voices: List<TtsVoice>,
    status: VoiceStatus?,
    onSelect: (String?) -> Unit,
) {
    val ready = status == VoiceStatus.INSTALLED_OFFLINE
    var open by remember { mutableStateOf(false) }
    val current = voices.firstOrNull { it.name == selected }?.label
        ?: stringResource(R.string.listen_voice_default)
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = ready) { open = true }
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.listen_voice_for, languageLabel(language)),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = if (ready || status == null) current else stringResource(R.string.voice_status_needs_download),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ready || status == null) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error,
                )
            }
            if (ready) {
                Icon(Icons.Rounded.UnfoldMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            KeepAppLocale {
                Column {
                    VoiceItem(stringResource(R.string.listen_voice_default), selected == null) {
                        onSelect(null); open = false
                    }
                    voices.forEach { voice ->
                        VoiceItem(voice.label, voice.name == selected) { onSelect(voice.name); open = false }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceItem(label: String, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        trailingIcon = {
            if (checked) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
    )
}
