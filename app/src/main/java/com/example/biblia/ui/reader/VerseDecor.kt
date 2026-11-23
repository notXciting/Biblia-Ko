package com.example.biblia.ui.reader

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp

/**
 * Rounded boxes behind one verse, line by line. Edges come from the verse's
 * own first/last CHARACTER boxes (the cursor AFTER the last char lands at the
 * start of the next paragraph in verse-by-verse layout, x = 0, which used to
 * drop the last line).
 */
internal fun DrawScope.drawVerseBoxes(l: TextLayoutResult, range: IntRange, color: Color) {
    val firstLine = l.getLineForOffset(range.first)
    val lastLine = l.getLineForOffset(range.last)
    val corner = 6.dp.toPx()
    for (line in firstLine..lastLine) {
        val left = if (line == firstLine) l.getBoundingBox(range.first).left else l.getLineLeft(line)
        val right = if (line == lastLine) l.getBoundingBox(range.last).right else l.getLineRight(line)
        drawRoundRect(
            color = color,
            topLeft = Offset(left, l.getLineTop(line)),
            size = Size((right - left).coerceAtLeast(0f), l.getLineBottom(line) - l.getLineTop(line)),
            cornerRadius = CornerRadius(corner, corner),
        )
    }
}

/**
 * Smoothly scrolls so the verse being read aloud is fully visible, leaving
 * [bottomReserve] px free above the floating Listen bar. No-op when visible.
 */
internal suspend fun scrollToSpokenVerse(
    flow: ChapterFlow,
    l: TextLayoutResult,
    state: ScrollState,
    verse: Int,
    textTop: Float,
    topMargin: Float,
    bottomReserve: Float,
) {
    val index = flow.indexOfVerse(verse)
    if (index < 0) return
    val range = flow.ranges[index]
    val top = textTop + l.getLineTop(l.getLineForOffset(range.first))
    val bottom = textTop + l.getLineBottom(l.getLineForOffset(range.last))
    val visibleTop = state.value + topMargin
    val visibleBottom = state.value + state.viewportSize - bottomReserve
    if (top < visibleTop || bottom > visibleBottom) {
        state.animateScrollTo((top - topMargin).toInt().coerceAtLeast(0))
    }
}

/** The note icon drawn inline after a verse that has a note (tap opens it). */
internal fun noteInlineContent(accent: Color): Map<String, InlineTextContent> = mapOf(
    NOTE_MARKER_ID to InlineTextContent(
        Placeholder(width = 1.1.em, height = 1.em, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter),
    ) {
        Icon(Icons.Outlined.EditNote, contentDescription = null, tint = accent, modifier = Modifier.fillMaxSize())
    },
)
