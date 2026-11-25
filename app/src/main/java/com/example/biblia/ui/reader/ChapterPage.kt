package com.example.biblia.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biblia.R
import com.example.biblia.ui.theme.LocalPageStyle
import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.domain.model.ChapterContent
import com.example.biblia.domain.model.NoteVerses
import com.example.biblia.domain.model.VerseLayout
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private const val SKELETON_DELAY_MS = 150L

/**
 * One pager page: chapter heading + flowing verses. The content starts from a
 * synchronous cache peek, so a preloaded chapter renders in the first frame;
 * only a genuine cache miss loads asynchronously and fades in.
 */
@Composable
fun ChapterPage(
    versionId: String,
    book: Int,
    chapter: Int,
    fontSizeSp: Float,
    lineSpacingMultiplier: Float,
    verseLayout: VerseLayout,
    isCurrentPage: Boolean,
    vm: ReaderViewModel,
    modifier: Modifier = Modifier,
    speakingVerse: Int = 0,
) {
    val scrollState = rememberScrollState()
    val initial = remember(versionId, book, chapter) { vm.peekChapter(versionId, book, chapter) }
    var content by remember(versionId, book, chapter) { mutableStateOf(initial) }
    val reveal = remember(versionId, book, chapter) { Animatable(if (initial != null) 1f else 0f) }
    // Placeholder only if the load is noticeably slow (fast loads never flash it).
    var showSkeleton by remember(versionId, book, chapter) { mutableStateOf(false) }
    LaunchedEffect(versionId, book, chapter) {
        if (content == null) {
            launch {
                delay(SKELETON_DELAY_MS)
                if (content == null) showSkeleton = true
            }
            content = vm.loadChapter(versionId, book, chapter)
            showSkeleton = false
            reveal.animateTo(1f, tween(durationMillis = 180))
        }
    }
    val haptics = LocalHapticFeedback.current
    val bookName = BookCatalog.book(book).displayName
    // Highlights for this chapter (user DB, version-independent), live.
    val highlightFlow = remember(book, chapter) { vm.actions.highlights(book, chapter) }
    val highlights by highlightFlow.collectAsState(initial = emptyMap())
    // Personal notes: an icon after the last verse of each note (user DB, live).
    val noteFlow = remember(book, chapter) { vm.actions.notes(book, chapter) }
    val notes by noteFlow.collectAsState(initial = emptyList())
    val noteMarkers = remember(notes, content) { NoteVerses.markers(notes, content?.spanEnds.orEmpty()) }
    val darkSurface = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val highlightColors = remember(highlights, darkSurface) {
        // 0.30 in dark themes keeps light verse text >= 5.6:1 on every highlight
        // (0.38 dropped yellow-on-Slate to 4.4:1); 0.55 on light themes.
        highlights.mapValues { (_, c) -> Color(c.argb).copy(alpha = if (darkSurface) 0.30f else 0.55f) }
    }

    // Printed-page themes (Paper) draw the chapter as a sheet on a desk.
    val page = LocalPageStyle.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .paperSheet(page)
            .verticalScroll(scrollState)
            .padding(horizontal = if (page != null) 20.dp else 24.dp),
    ) {
        Spacer(Modifier.height(28.dp))
        ChapterHeading(bookName, stringResource(R.string.selection_chapter_breadcrumb, chapter), page)
        Spacer(Modifier.height(20.dp))
        val verses = content?.verses.orEmpty()
        AnimatedVisibility(visible = showSkeleton, enter = fadeIn(tween(200)), exit = fadeOut(tween(120))) {
            ChapterSkeleton(fontSize = fontSizeSp.sp, lineHeightMultiplier = lineSpacingMultiplier)
        }
        if (verses.isNotEmpty()) {
            ChapterFlowText(
                verses = verses,
                fontSize = fontSizeSp.sp,
                lineHeightMultiplier = lineSpacingMultiplier * if (page != null) PAPER_LINE_HEIGHT else 1f,
                accent = MaterialTheme.colorScheme.primary,
                numberColor = page?.verseNumber ?: MaterialTheme.colorScheme.primary,
                balanced = page != null,
                layout = verseLayout,
                selectedVerses = if (isCurrentPage) vm.selectedVerses else emptySet(),
                highlights = highlightColors,
                pulseVerse = if (isCurrentPage) vm.pulseVerse else 0,
                anchorVerse = if (isCurrentPage) vm.anchorVerse else 0,
                onAnchorConsumed = { vm.consumeAnchor() },
                onTopVerse = if (isCurrentPage) vm::onTopVerse else null,
                speakingVerse = speakingVerse,
                notedVerses = noteMarkers.keys,
                onNoteTap = { verse -> noteMarkers[verse]?.firstOrNull()?.let { vm.openNote(it) } },
                onVerseTap = { number ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isCurrentPage) vm.tapVerse(number)
                },
                onPulseFinished = { vm.consumePulse() },
                scrollState = scrollState,
                modifier = Modifier.graphicsLayer { alpha = reveal.value },
            )
        }
        // "About this translation" at the end of every chapter.
        vm.versionMeta?.let { meta ->
            Spacer(Modifier.height(36.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.reader_copyright_heading),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = vm.version.fullName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = meta.copyright,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (meta.link.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = meta.link,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        // Room to scroll the last verses above the verse panel.
        Spacer(Modifier.height(220.dp))
    }
}
