package com.example.biblia.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.tts.VoiceStatus
import com.example.biblia.ui.listen.languageLabel

/**
 * "Voice status": one line per Bible language — Installed offline / Needs
 * download / Engine missing. A line that isn't ready opens the help dialog.
 */
@Composable
internal fun VoiceStatusRows(
    languages: List<String>,
    status: Map<String, VoiceStatus>,
    onHelp: (language: String, status: VoiceStatus) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            text = stringResource(R.string.voice_status_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        languages.forEach { language ->
            val s = status[language]
            val (icon, label, color) = presentation(s)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = s != null && s != VoiceStatus.INSTALLED_OFFLINE) {
                        s?.let { onHelp(language, it) }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = languageLabel(language),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(text = label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = color)
            }
        }
    }
}

@Composable
private fun presentation(status: VoiceStatus?): Triple<ImageVector, String, Color> {
    val colors = MaterialTheme.colorScheme
    return when (status) {
        null -> Triple(Icons.Outlined.HourglassEmpty, stringResource(R.string.voice_status_checking), colors.onSurfaceVariant)
        VoiceStatus.INSTALLED_OFFLINE ->
            Triple(Icons.Rounded.CheckCircle, stringResource(R.string.voice_status_installed), colors.primary)
        VoiceStatus.NEEDS_DOWNLOAD ->
            Triple(Icons.Outlined.CloudDownload, stringResource(R.string.voice_status_needs_download), colors.error)
        VoiceStatus.ENGINE_MISSING ->
            Triple(Icons.Outlined.ErrorOutline, stringResource(R.string.voice_status_engine_missing), colors.error)
    }
}
