package com.example.biblia.ui.selection

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.biblia.R
import com.example.biblia.domain.model.BookFilter
import com.example.biblia.domain.model.ViewMode

/**
 * Back / FILTER / SEARCH / VIEW row. Tapping search crossfades into a rounded,
 * auto-focused search field with a clear/close button.
 */
@Composable
fun SelectionTopRow(
    filter: BookFilter,
    searchActive: Boolean,
    query: String,
    viewMode: ViewMode,
    onFilterClick: () -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onViewToggle: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(searchActive) {
        if (searchActive) {
            withFrameNanos { } // field is attached after the first frame of the crossfade
            runCatching { focusRequester.requestFocus() }
        }
    }
    AnimatedContent(
        targetState = searchActive,
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
        label = "topRow",
        modifier = modifier.fillMaxWidth(),
    ) { searching ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (searching) {
                SearchField(query, onQueryChange, onSearchToggle, focusRequester, Modifier.weight(1f))
            } else {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                }
                Surface(
                    onClick = { onSearchToggle(true) },
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f).height(44.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.action_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.action_search),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                BadgedBox(badge = { if (filter != BookFilter.ALL) Badge() }) {
                    IconButton(onClick = onFilterClick) {
                        Icon(Icons.Filled.FilterList, stringResource(R.string.action_filter))
                    }
                }
                IconButton(onClick = onViewToggle) {
                    Icon(
                        imageVector = if (viewMode == ViewMode.LIST) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
                        contentDescription = stringResource(R.string.action_toggle_view),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchToggle: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        shape = RoundedCornerShape(50),
        placeholder = {
            Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            IconButton(onClick = { onQueryChange(""); onSearchToggle(false) }) {
                Icon(Icons.Filled.Close, stringResource(R.string.action_clear_search))
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.focusRequester(focusRequester),
    )
}
