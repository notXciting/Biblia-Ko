package com.example.biblia.ui.selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.biblia.R

/** "Mga Aklat > Juan > Kabanata 3"; intermediate steps are tappable. */
@Composable
fun Breadcrumb(
    level: SelectionLevel,
    bookName: String,
    chapter: Int,
    onBooksClick: () -> Unit,
    onChaptersClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BreadcrumbItem(
            label = stringResource(R.string.selection_breadcrumb_books),
            isLast = level == SelectionLevel.BOOKS,
            onClick = onBooksClick,
        )
        if (level != SelectionLevel.BOOKS) {
            Separator()
            BreadcrumbItem(
                label = bookName,
                isLast = level == SelectionLevel.CHAPTERS,
                onClick = onChaptersClick,
            )
        }
        if (level == SelectionLevel.VERSES) {
            Separator()
            BreadcrumbItem(
                label = stringResource(R.string.selection_chapter_breadcrumb, chapter),
                isLast = true,
                onClick = {},
            )
        }
    }
}

@Composable
private fun BreadcrumbItem(label: String, isLast: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Medium,
        color = if (isLast) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.primary
        },
        maxLines = 1,
        modifier = if (isLast) Modifier else Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun Separator() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(horizontal = 4.dp).size(16.dp),
    )
}
