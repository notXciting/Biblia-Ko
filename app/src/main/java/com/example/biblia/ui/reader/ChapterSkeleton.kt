package com.example.biblia.ui.reader

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/** Line widths of a paragraph-shaped placeholder (last line of each "verse" shorter). */
private val LINE_WIDTHS = floatArrayOf(
    0.96f, 0.92f, 0.98f, 0.55f, 0.94f, 0.97f, 0.9f, 0.72f, 0.95f, 0.88f, 0.98f, 0.4f,
)

/**
 * Shown while a chapter's text is still loading: lines sized like the reading
 * text that gently pulse (alpha animated in the draw phase, no recomposition).
 */
@Composable
fun ChapterSkeleton(fontSize: TextUnit, lineHeightMultiplier: Float, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    val density = LocalDensity.current
    val barHeight = with(density) { (fontSize.toPx() * 0.62f).toDp() }
    val gap = with(density) { (fontSize.toPx() * lineHeightMultiplier).toDp() } - barHeight
    val color = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(
        verticalArrangement = Arrangement.spacedBy(gap.coerceAtLeast(6.dp)),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = pulse },
    ) {
        LINE_WIDTHS.forEach { width ->
            Box(
                Modifier
                    .fillMaxWidth(width)
                    .height(barHeight)
                    .background(color, RoundedCornerShape(50)),
            )
        }
    }
}
