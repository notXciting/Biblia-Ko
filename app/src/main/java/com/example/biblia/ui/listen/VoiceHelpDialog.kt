package com.example.biblia.ui.listen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.tts.VoiceProblem
import com.example.biblia.domain.tts.VoiceStatus
import com.example.biblia.ui.KeepAppLocale

/**
 * Friendly help when a language can't be read aloud offline: what's wrong,
 * three steps, and buttons for the voice installer (or the Play Store when
 * there is no engine at all) and the phone's Text-to-speech settings.
 */
@Composable
fun VoiceHelpDialog(problem: VoiceProblem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val noEngine = problem.status == VoiceStatus.ENGINE_MISSING
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null) },
        title = {
            KeepAppLocale {
                val language = languageLabel(problem.language)
                Text(
                    if (noEngine) stringResource(R.string.voice_engine_title)
                    else stringResource(R.string.voice_offline_title, language),
                )
            }
        },
        text = { KeepAppLocale { HelpBody(problem.language, noEngine) } },
        confirmButton = {
            KeepAppLocale {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(onClick = {
                        if (noEngine) openSpeechServicesListing(context) else openVoiceInstaller(context)
                        onDismiss()
                    }) {
                        Text(stringResource(if (noEngine) R.string.voice_open_play_store else R.string.voice_install))
                    }
                    OutlinedButton(onClick = { openTtsSettings(context); onDismiss() }) {
                        Text(stringResource(R.string.voice_open_tts_settings))
                    }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.voice_later)) }
                }
            }
        },
    )
}

@Composable
private fun HelpBody(language: String, noEngine: Boolean) {
    val label = languageLabel(language)
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(
            text = if (noEngine) stringResource(R.string.voice_engine_body)
            else stringResource(R.string.voice_offline_body, label),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.voice_steps_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
        Step(1, stringResource(R.string.voice_step_1))
        Step(2, stringResource(R.string.voice_step_2))
        Step(3, stringResource(R.string.voice_step_3, label))
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = "$number.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(20.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/** "Tagalog" / "Ingles" (English) in the current UI language. */
@Composable
fun languageLabel(language: String): String = stringResource(
    if (language == "Tagalog") R.string.listen_language_tagalog else R.string.listen_language_english,
)
