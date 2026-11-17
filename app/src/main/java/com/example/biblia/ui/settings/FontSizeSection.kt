package com.example.biblia.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biblia.R
import com.example.biblia.domain.model.LineSpacing
import kotlin.math.roundToInt

/**
 * Font size: live preview verse (Juan 3:16) that grows while dragging, plus a
 * 12..32sp slider persisted to DataStore on release.
 */
@Composable
fun FontSizeSection(
    fontSizeSp: Float,
    lineSpacing: LineSpacing,
    previewReference: String? = null,
    previewText: String? = null,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sliderLabel = stringResource(R.string.settings_font_size)
    var slider by remember(fontSizeSp) { mutableStateOf(fontSizeSp) }
    Column(modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.settings_font_size))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_font_preview),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = previewReference ?: stringResource(R.string.font_preview_reference),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = previewText ?: stringResource(R.string.font_preview_text),
                    fontSize = slider.sp,
                    lineHeight = (slider * lineSpacing.multiplier).sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_font_small_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = slider,
                onValueChange = { slider = it },
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
        Text(
            text = stringResource(R.string.settings_font_value, slider.roundToInt()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
