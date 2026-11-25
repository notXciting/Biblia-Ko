package com.example.biblia.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.example.biblia.R

/**
 * Font size: a 12..32sp slider. [onLive] reports every drag step (the page's
 * preview grows with it); [onChange] persists on release.
 */
@Composable
fun FontSizeSection(
    fontSizeSp: Float,
    onLive: (Float) -> Unit,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sliderLabel = stringResource(R.string.settings_font_size)
    var slider by remember(fontSizeSp) { mutableStateOf(fontSizeSp) }
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(sliderLabel, Modifier.weight(1f))
            Text(
                text = stringResource(R.string.settings_font_value, slider.roundToInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_font_small_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = slider,
                onValueChange = {
                    slider = it
                    onLive(it)
                },
                valueRange = 12f..32f,
                onValueChangeFinished = { onChange(slider) },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .semantics { contentDescription = sliderLabel },
            )
            Text(
                text = stringResource(R.string.settings_font_large_label),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
