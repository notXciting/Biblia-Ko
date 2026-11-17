package com.example.biblia.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.biblia.R
import com.example.biblia.domain.tts.TtsPrefs
import com.example.biblia.domain.tts.TtsVoice
import com.example.biblia.domain.tts.VoiceStatus
import java.util.Locale

/**
 * "Listen" settings: speed, pitch, a voice per language, verse numbers and a
 * test button. Voices are re-checked on every resume, so returning from the
 * voice installer updates the list.
 */
@Composable
fun ListenSection(
    prefs: TtsPrefs,
    languages: List<String>,
    voices: Map<String, List<TtsVoice>>,
    status: Map<String, VoiceStatus>,
    onRefresh: () -> Unit,
    onVoiceHelp: (language: String, status: VoiceStatus) -> Unit,
    onRate: (Float) -> Unit,
    onPitch: (Float) -> Unit,
    onVoice: (language: String, voiceName: String?) -> Unit,
    onSpeakNumbers: (Boolean) -> Unit,
    onTest: () -> Unit,
) {
    LifecycleResumeEffect(Unit) {
        onRefresh()
        onPauseOrDispose { }
    }
    Column(Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.settings_listen))
        Text(
            text = stringResource(R.string.listen_settings_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        VoiceStatusRows(languages = languages, status = status, onHelp = onVoiceHelp)
        ValueSlider(
            label = stringResource(R.string.listen_speed),
            value = prefs.rate,
            range = TtsPrefs.MIN_RATE..TtsPrefs.MAX_RATE,
            steps = 14, // 0.05 increments
            format = { String.format(Locale.US, "%.2f×", it) },
            onChange = onRate,
        )
        ValueSlider(
            label = stringResource(R.string.listen_pitch),
            value = prefs.pitch,
            range = TtsPrefs.MIN_PITCH..TtsPrefs.MAX_PITCH,
            steps = 9,
            format = { String.format(Locale.US, "%.2f", it) },
            onChange = onPitch,
        )
        languages.forEach { language ->
            VoicePickerRow(
                language = language,
                selected = prefs.voices[language],
                voices = voices[language].orEmpty(),
                status = status[language],
                onSelect = { onVoice(language, it) },
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text(
                text = stringResource(R.string.listen_speak_numbers),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = prefs.speakVerseNumbers, onCheckedChange = onSpeakNumbers)
        }
        FilledTonalButton(onClick = onTest, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.listen_test_voice))
        }
    }
}

/** Labelled slider that persists only when the drag ends. */
@Composable
private fun ValueSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    var current by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                text = format(current),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = current,
            onValueChange = { current = it },
            valueRange = range,
            steps = steps,
            onValueChangeFinished = { onChange(current) },
        )
    }
}
