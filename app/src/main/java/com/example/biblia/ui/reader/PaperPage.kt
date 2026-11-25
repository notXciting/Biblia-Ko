package com.example.biblia.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.ui.theme.PageStyle

/**
 * The reader page as a sheet of paper on a desk (Paper theme): desk colour
 * around it, soft side margins, a very subtle shadow, and optionally a faint
 * paper grain (a small bundled tile, drawn once per frame as a repeated shader).
 */
@Composable
internal fun Modifier.paperSheet(page: PageStyle?): Modifier {
    if (page == null) return this
    val paperColor = MaterialTheme.colorScheme.background
    val grain = if (page.grain) rememberGrainBrush() else null
    return this
        .background(page.desk)
        .padding(horizontal = 10.dp, vertical = 6.dp)
        .shadow(elevation = 2.dp, shape = RoundedCornerShape(3.dp))
        .background(paperColor)
        .drawBehind { grain?.let { drawRect(it, alpha = GRAIN_ALPHA, blendMode = BlendMode.Multiply) } }
}

@Composable
private fun rememberGrainBrush(): ShaderBrush {
    val tile: ImageBitmap = ImageBitmap.imageResource(R.drawable.paper_grain)
    return remember(tile) { ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated)) }
}

/** Subtle paper texture: visible up close, quiet while reading. */
private const val GRAIN_ALPHA = 0.25f

/** Printed-page text: a touch more line height than the chosen spacing. */
internal const val PAPER_LINE_HEIGHT = 1.08f
