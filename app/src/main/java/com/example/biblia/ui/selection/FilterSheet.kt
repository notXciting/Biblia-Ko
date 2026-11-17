@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.example.biblia.ui.selection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.BookFilter
import com.example.biblia.domain.model.FilterGroup
import com.example.biblia.ui.KeepAppLocale

/**
 * Filter sheet: chips grouped under General / Old Testament / New Testament /
 * Themes, each chip showing how many books it contains.
 */
@Composable
fun FilterSheet(
    current: BookFilter,
    onSelect: (BookFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { KeepAppLocale {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.filter_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            FilterGroup.entries.forEach { group ->
                Text(
                    text = stringResource(groupLabel(group)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    BookFilter.entries.filter { it.group == group }.forEach { value ->
                        FilterChip(
                            selected = value == current,
                            onClick = { onSelect(value) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(filterLabel(value)))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = value.bookCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    } }
}

private fun groupLabel(group: FilterGroup): Int = when (group) {
    FilterGroup.GENERAL -> R.string.filter_group_general
    FilterGroup.OLD_TESTAMENT -> R.string.filter_old_testament
    FilterGroup.NEW_TESTAMENT -> R.string.filter_new_testament
    FilterGroup.THEMES -> R.string.filter_group_themes
}

/** String resource for a filter's label (also used by the active-filter bar). */
internal fun filterLabel(filter: BookFilter): Int = when (filter) {
    BookFilter.ALL -> R.string.filter_all
    BookFilter.OLD_TESTAMENT -> R.string.filter_old_testament
    BookFilter.NEW_TESTAMENT -> R.string.filter_new_testament
    BookFilter.LAW -> R.string.filter_law
    BookFilter.HISTORY -> R.string.filter_history
    BookFilter.POETRY -> R.string.filter_poetry
    BookFilter.MAJOR_PROPHETS -> R.string.filter_major_prophets
    BookFilter.MINOR_PROPHETS -> R.string.filter_minor_prophets
    BookFilter.GOSPELS -> R.string.filter_gospels
    BookFilter.PAULS_LETTERS -> R.string.filter_pauls_letters
    BookFilter.GENERAL_LETTERS -> R.string.filter_general_letters
    BookFilter.ALL_LETTERS -> R.string.filter_all_letters
    BookFilter.NARRATIVE -> R.string.filter_narrative
    BookFilter.PROPHECY -> R.string.filter_prophecy
    BookFilter.PRISON_LETTERS -> R.string.filter_prison_letters
    BookFilter.PASTORAL_LETTERS -> R.string.filter_pastoral_letters
    BookFilter.ONE_CHAPTER -> R.string.filter_one_chapter
}
