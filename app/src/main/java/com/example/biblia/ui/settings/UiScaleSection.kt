package com.example.biblia.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.UiScale
import kotlin.math.roundToInt

/**
 * "Laki ng app": scales the WHOLE app (buttons, menus, text), on top of the
 * phone's own display size. Applied when the slider is released, so the
 * screen doesn't resize under the finger while dragging.
 */
@Composable
fun UiScaleSection(current: Float, onChange: (Float) -> Unit) {
    var value by remember(current) { mutableFloatStateOf(current) }
    val percent = (value * 100).roundToInt()
    val label = stringResource(R.string.settings_ui_scale)
    Column(Modifier.fillMaxWidth()) {
        SectionTitle(label)
        Text(
            text = stringResource(R.string.settings_ui_scale_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp),
        ) {
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { value = UiScale.DEFAULT; onChange(UiScale.DEFAULT) },
                enabled = percent != 100,
            ) { Text(stringResource(R.string.settings_ui_scale_reset)) }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Icon(Icons.Outlined.ZoomOut, contentDescription = null, modifier = Modifier.size(20.dp))
            Slider(
                value = value,
                onValueChange = { value = UiScale.clamp(it) },
                valueRange = UiScale.MIN..UiScale.MAX,
                steps = ((UiScale.MAX - UiScale.MIN) / UiScale.STEP).roundToInt() - 1,
                onValueChangeFinished = { onChange(value) },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .semantics { contentDescription = label },
            )
            Icon(Icons.Outlined.ZoomIn, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}
