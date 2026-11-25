package com.example.biblia.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow

/**
 * [full] when it fits in [maxWidthPx] on one line, else [short]
 * ("1 Tesalonica 4:16" -> "1 Tes 4:16"). Measured before drawing, so the
 * long form never flashes.
 */
@Composable
fun fittingLabel(full: String, short: String, style: TextStyle, maxWidthPx: Int): String {
    val measurer = rememberTextMeasurer()
    return remember(full, short, style, maxWidthPx) {
        if (measurer.measure(full, style, maxLines = 1).size.width <= maxWidthPx) full else short
    }
}

/** One-line text that switches to [short] when [full] doesn't fit the width it gets. */
@Composable
fun FittingText(full: String, short: String, style: TextStyle, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    BoxWithConstraints(modifier) {
        Text(
            text = fittingLabel(full, short, style, constraints.maxWidth),
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
