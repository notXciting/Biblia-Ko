package com.example.biblia.ui.reader

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseLayout
import com.example.biblia.domain.model.VerseSpans
import com.example.biblia.perf.VersePerf

internal class ChapterFlow(
    val annotated: AnnotatedString,
    val ranges: List<IntRange>,
    val numbers: List<Int>,
    /** Combined verses ("1-2"): text verse -> last verse it covers. */
    val ends: Map<Int, Int> = emptyMap(),
) {
    /** Index of the verse that shows [verse]; a merged number maps to its combined verse. */
    fun indexOfVerse(verse: Int): Int {
        val i = numbers.indexOf(verse)
        return if (i >= 0) i else numbers.indexOf(VerseSpans.ownerOf(verse, ends))
    }
}

internal fun buildChapterFlow(
    verses: List<Verse>,
    accent: Color,
    fontSize: TextUnit,
    layout: VerseLayout,
): ChapterFlow {
    val ends = VerseSpans.ends(verses)
    return if (layout == VerseLayout.VERSE_BY_VERSE) buildVerseByVerse(verses, accent, fontSize, ends)
    else buildParagraph(verses, accent, fontSize, ends)
}

private const val EN_SPACE = ' '
private const val ZERO_WIDTH_SPACE = '​'

/** One flowing paragraph: superscript numbers inline. */
private fun buildParagraph(verses: List<Verse>, accent: Color, fontSize: TextUnit, ends: Map<Int, Int>): ChapterFlow {
    val builder = AnnotatedString.Builder()
    val ranges = ArrayList<IntRange>()
    val numbers = ArrayList<Int>()
    val numberSize = (fontSize.value * 0.62f).sp
    for (verse in verses) {
        if (verse.text.isBlank()) continue
        val start = builder.length
        builder.withStyle(
            SpanStyle(
                color = accent,
                fontSize = numberSize,
                fontWeight = FontWeight.SemiBold,
                baselineShift = BaselineShift.Superscript,
            )
        ) { append(VerseSpans.label(verse.verse, ends)) }
        builder.append(' ')
        builder.append(verse.text)
        ranges.add(start until builder.length)
        numbers.add(verse.verse)
        builder.append(' ')
    }
    return ChapterFlow(builder.toAnnotatedString(), ranges, numbers, ends)
}

/**
 * One paragraph per verse, a bold accent number leading each, separated by a
 * half-line gap. The gap is a tiny paragraph with its own (small) lineHeight,
 * so it is a real gap rather than a full blank line. Verse ranges never cover
 * the gaps, so tapping, highlights and the pulse work exactly as in PARAGRAPH.
 */
private fun buildVerseByVerse(verses: List<Verse>, accent: Color, fontSize: TextUnit, ends: Map<Int, Int>): ChapterFlow {
    val builder = AnnotatedString.Builder()
    val ranges = ArrayList<IntRange>()
    val numbers = ArrayList<Int>()
    val numberStyle = SpanStyle(
        color = accent,
        fontSize = (fontSize.value * 0.78f).sp,
        fontWeight = FontWeight.Bold,
    )
    val gap = ParagraphStyle(lineHeight = (fontSize.value * 0.55f).sp)
    val gapText = SpanStyle(fontSize = (fontSize.value * 0.3f).sp)
    val visible = verses.filter { it.text.isNotBlank() }
    visible.forEachIndexed { i, verse ->
        builder.withStyle(ParagraphStyle()) {
            val start = length
            withStyle(numberStyle) { append(VerseSpans.label(verse.verse, ends)) }
            append(EN_SPACE) // between number and text
            append(verse.text)
            ranges.add(start until length)
            numbers.add(verse.verse)
        }
        if (i < visible.lastIndex) {
            builder.withStyle(gap) { withStyle(gapText) { append(ZERO_WIDTH_SPACE) } }
        }
    }
    return ChapterFlow(builder.toAnnotatedString(), ranges, numbers, ends)
}

/**
 * Flowing paragraph-style chapter text: inline superscript verse numbers,
 * per-verse tap selection, highlight for selected verses and a one-shot pulse
 * highlight drawn in the DRAW PHASE (drawBehind reads the Animatable value, so
 * the pulse animates without recomposing per frame). The base AnnotatedString
 * is built once per chapter inside remember.
 */
@Composable
fun ChapterFlowText(
    verses: List<Verse>,
    fontSize: TextUnit,
    lineHeightMultiplier: Float,
    accent: Color,
    layout: VerseLayout = VerseLayout.PARAGRAPH,
    selectedVerses: Set<Int>,
    highlights: Map<Int, Color> = emptyMap(),
    pulseVerse: Int,
    onVerseTap: (Int) -> Unit,
    onPulseFinished: () -> Unit,
    scrollState: ScrollState? = null,
    anchorVerse: Int = 0,
    onAnchorConsumed: () -> Unit = {},
    onTopVerse: ((Int) -> Unit)? = null,
    speakingVerse: Int = 0,
    modifier: Modifier = Modifier,
) {
    // Scroll target: a highlighted jump verse, else a silent anchor (version switch).
    val scrollTarget = if (pulseVerse > 0) pulseVerse else anchorVerse
    val reportTop by rememberUpdatedState(onTopVerse)
    val flow = remember(verses, accent, fontSize, layout) { buildChapterFlow(verses, accent, fontSize, layout) }
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(pulseVerse) {
        if (pulseVerse > 0) {
            pulse.snapTo(0f)
            pulse.animateTo(0.30f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
            pulse.animateTo(0f, tween(durationMillis = 1600, delayMillis = 500, easing = LinearOutSlowInEasing))
            onPulseFinished()
        }
    }
    val selectionColor = accent.copy(alpha = 0.16f)
    val speakingColor = accent.copy(alpha = 0.14f)
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var textTop by remember { mutableFloatStateOf(0f) }
    var scrolledFor by remember { mutableIntStateOf(0) }
    // Scroll value right after a programmatic positioning (plain holder: not UI state).
    val settledAt = remember { intArrayOf(-1) }
    // A page opened AT a verse stays invisible until it has been scrolled
    // there, so the user never sees the top of the chapter jump away.
    var positioned by remember { mutableStateOf(scrollTarget <= 0 || scrollState == null) }
    val margin = with(LocalDensity.current) { 32.dp.toPx() }
    val listenBarReserve = with(LocalDensity.current) { 140.dp.toPx() }

    LaunchedEffect(Unit) {
        delay(300) // safety net: never stay hidden
        positioned = true
    }
    // Scroll once per pulse target so the verse sits near the top.
    LaunchedEffect(flow, scrollTarget, layout) {
        val l = layout ?: return@LaunchedEffect
        val state = scrollState ?: return@LaunchedEffect
        if (scrollTarget <= 0 || scrolledFor == scrollTarget) return@LaunchedEffect
        val index = flow.indexOfVerse(scrollTarget)
        if (index >= 0) {
            val line = l.getLineForOffset(flow.ranges[index].first)
            val target = (textTop + l.getLineTop(line) - margin).coerceAtLeast(0f)
            state.scrollTo(target.toInt())
            // Near the chapter end the scroll is clamped; keep reporting the
            // requested verse until the user actually scrolls (no drift).
            settledAt[0] = state.value
            VersePerf.stage("verse scrolled into view")
        }
        scrolledFor = scrollTarget
        positioned = true
        if (pulseVerse <= 0) onAnchorConsumed()
    }
    // "Listen": keep the verse being read in view (smooth; only when needed).
    LaunchedEffect(flow, layout, speakingVerse) {
        val l = layout ?: return@LaunchedEffect
        val state = scrollState ?: return@LaunchedEffect
        if (speakingVerse > 0) scrollToSpokenVerse(flow, l, state, speakingVerse, textTop, margin, listenBarReserve)
    }
    // Report the verse at the top of the viewport (same probe line the scroll
    // above targets, so switching back and forth never drifts).
    LaunchedEffect(flow, layout, onTopVerse != null) {
        val l = layout ?: return@LaunchedEffect
        val state = scrollState ?: return@LaunchedEffect
        if (onTopVerse == null) return@LaunchedEffect
        snapshotFlow { if (positioned) state.value else -1 }.collect { value ->
            if (value < 0) return@collect
            if (value == settledAt[0]) return@collect
            settledAt[0] = -1
            val y = value - textTop + margin + 1f
            reportTop?.invoke(if (y <= 0f) 0 else topVerseAt(flow, l, y))
        }
    }

    Text(
        text = remember(flow, selectedVerses, highlights, selectionColor) {
            buildStyled(flow, selectedVerses, selectionColor, highlights)
        },
        style = TextStyle(
            fontFamily = FontFamily.Serif,
            fontSize = fontSize,
            lineHeight = fontSize * lineHeightMultiplier,
            color = MaterialTheme.colorScheme.onBackground,
        ),
        onTextLayout = { layout = it },
        modifier = modifier
            .fillMaxWidth()
            .onPlaced { textTop = it.positionInParent().y }
            .graphicsLayer { alpha = if (positioned) 1f else 0f }
            .drawBehind {
                val l = layout ?: return@drawBehind
                val spoken = flow.indexOfVerse(speakingVerse)
                if (speakingVerse > 0 && spoken >= 0) drawVerseBoxes(l, flow.ranges[spoken], speakingColor)
                val alpha = pulse.value
                if (alpha <= 0f || pulseVerse <= 0) return@drawBehind
                val index = flow.indexOfVerse(pulseVerse)
                if (index < 0) return@drawBehind
                drawVerseBoxes(l, flow.ranges[index], accent.copy(alpha = alpha))
            }
            .pointerInput(flow) {
                detectTapGestures { position ->
                    val l = layout ?: return@detectTapGestures
                    val offset = l.getOffsetForPosition(position)
                    val index = flow.ranges.indexOfFirst { offset in it }
                    if (index >= 0) onVerseTap(flow.numbers[index])
                }
            },
    )
}

/**
 * Verse for the line at [y]: the first verse that STARTS on that line, else the
 * verse the line is in the middle of.
 */
private fun topVerseAt(flow: ChapterFlow, l: TextLayoutResult, y: Float): Int {
    if (flow.ranges.isEmpty()) return 0
    val line = l.getLineForVerticalPosition(y)
    val lineStart = l.getLineStart(line)
    val lineEnd = l.getLineEnd(line)
    var containing = flow.numbers.first()
    for (i in flow.ranges.indices) {
        val start = flow.ranges[i].first
        if (start in lineStart until lineEnd) return flow.numbers[i]
        if (start <= lineStart) containing = flow.numbers[i] else break
    }
    return containing
}

/**
 * Base chapter text + persistent highlight backgrounds + the temporary
 * selection (tint + underline, so it stays visible over a highlight).
 */
private fun buildStyled(
    flow: ChapterFlow,
    selected: Set<Int>,
    selectionColor: Color,
    highlights: Map<Int, Color>,
): AnnotatedString {
    if (selected.isEmpty() && highlights.isEmpty()) return flow.annotated
    val builder = AnnotatedString.Builder(flow.annotated)
    for (i in flow.numbers.indices) {
        val n = flow.numbers[i]
        val from = flow.ranges[i].first
        val to = flow.ranges[i].last + 1
        // A combined verse ("1-2") shows a highlight made on any of its numbers
        // (e.g. on verse 2 in a version that has it separately).
        val color = highlights[n] ?: (n + 1..VerseSpans.end(n, flow.ends)).firstNotNullOfOrNull { highlights[it] }
        color?.let { builder.addStyle(SpanStyle(background = it), from, to) }
        if (n in selected) {
            builder.addStyle(
                SpanStyle(background = selectionColor, textDecoration = TextDecoration.Underline),
                from,
                to,
            )
        }
    }
    return builder.toAnnotatedString()
}
