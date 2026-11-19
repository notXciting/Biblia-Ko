@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.biblia.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biblia.R
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseSpans
import com.example.biblia.ui.KeepAppLocale

/** The selected verses in every version, one block per version. */
@Composable
fun CompareSheet(state: CompareState, currentVersionId: String, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        KeepAppLocale {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
            ) {
                Text(
                    text = stringResource(R.string.compare_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = state.reference,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                state.rows.forEachIndexed { index, row ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    CompareBlock(row, isCurrent = row.version.id == currentVersionId)
                }
            }
        }
    }
}

@Composable
private fun CompareBlock(row: CompareRow, isCurrent: Boolean) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.padding(vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 52.dp, height = 26.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCurrent) colors.primary else colors.surfaceContainerHighest),
            ) {
                Text(
                    text = row.version.shortName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) colors.onPrimary else colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = row.version.fullName,
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        val verses = row.verses
        if (verses == null) {
            Text(
                text = stringResource(R.string.compare_not_installed),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        } else {
            Text(
                text = numberedText(verses, row.spanEnds, colors.primary),
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif),
                color = colors.onSurface,
            )
        }
    }
}

/** "¹⁶ text ¹⁷ text" with small accent verse numbers (only when several verses). */
private fun numberedText(verses: List<Verse>, ends: Map<Int, Int>, accent: androidx.compose.ui.graphics.Color) =
    buildAnnotatedString {
        verses.forEachIndexed { i, v ->
            if (i > 0) append(' ')
            if (verses.size > 1) {
                withStyle(SpanStyle(color = accent, fontSize = 11.sp, baselineShift = BaselineShift.Superscript)) {
                    append(VerseSpans.label(v.verse, ends))
                }
                append(' ')
            }
            append(v.text)
        }
    }
