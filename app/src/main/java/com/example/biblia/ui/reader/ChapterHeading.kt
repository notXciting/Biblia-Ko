package com.example.biblia.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biblia.ui.theme.PageStyle

/**
 * Chapter heading. Default: accent book label, serif "Kabanata 3", short
 * accent bar. With a printed-page theme ([page], Paper): centred, the book in
 * spaced serif capitals in the rubric red, and a hairline rule with a small
 * diamond ornament, like a printed Bible.
 */
@Composable
internal fun ChapterHeading(bookName: String, chapterTitle: String, page: PageStyle?) {
    if (page == null) {
        Column {
            Text(
                text = bookName.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = chapterTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                thickness = 2.dp,
                modifier = Modifier.width(40.dp),
            )
        }
        return
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = bookName.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            fontFamily = FontFamily.Serif,
            letterSpacing = 3.sp,
            color = page.verseNumber,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = chapterTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Serif,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        OrnamentRule(page)
    }
}

/** ———— ◆ ———— : hairlines with a small diamond, drawn (no font glyph needed). */
@Composable
private fun OrnamentRule(page: PageStyle) {
    val rule = MaterialTheme.colorScheme.outline
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(color = rule, thickness = 0.5.dp, modifier = Modifier.width(56.dp))
        Canvas(Modifier.size(width = 18.dp, height = 8.dp)) {
            val cx = size.width / 2
            val cy = size.height / 2
            val r = size.height / 2
            val diamond = Path().apply {
                moveTo(cx, cy - r); lineTo(cx + r, cy); lineTo(cx, cy + r); lineTo(cx - r, cy); close()
            }
            drawPath(diamond, page.verseNumber)
            drawCircle(rule, radius = 0.8.dp.toPx(), center = Offset(cx - r - 3.dp.toPx(), cy))
            drawCircle(rule, radius = 0.8.dp.toPx(), center = Offset(cx + r + 3.dp.toPx(), cy))
        }
        HorizontalDivider(color = rule, thickness = 0.5.dp, modifier = Modifier.width(56.dp))
    }
}
