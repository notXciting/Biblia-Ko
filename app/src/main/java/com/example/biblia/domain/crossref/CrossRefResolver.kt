package com.example.biblia.domain.crossref

import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseSpans

/** A target as shown in the current Bible version (combined verses widened). */
data class ResolvedRef(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val endChapter: Int,
    val endVerse: Int,
)

/**
 * Fits language-independent targets onto the current version. Some versions
 * merge verses (MBB12/ASD: Ruth 1:1 holds 1-2), so a target on a merged
 * number starts at its combined verse and a range ends where the last
 * combined verse ends. Pure, unit-tested.
 */
object CrossRefResolver {

    /** [startEnds]/[endEnds]: combined verses (VerseSpans.ends) of the start/end chapter. */
    fun resolve(target: CrossRefTarget, startEnds: Map<Int, Int>, endEnds: Map<Int, Int>): ResolvedRef {
        val start = VerseSpans.ownerOf(target.verse, startEnds)
        val end = VerseSpans.end(VerseSpans.ownerOf(target.endVerse, endEnds), endEnds)
        val sameChapter = target.endChapter == target.chapter
        return ResolvedRef(target.book, target.chapter, start, target.endChapter, if (sameChapter) maxOf(end, start) else end)
    }

    /** "Ruth 1:1-2", "Juan 3:16", "Genesis 1:31-2:3". */
    fun label(bookName: String, r: ResolvedRef): String = when {
        r.endChapter != r.chapter -> "$bookName ${r.chapter}:${r.verse}-${r.endChapter}:${r.endVerse}"
        r.endVerse > r.verse -> "$bookName ${r.chapter}:${r.verse}-${r.endVerse}"
        else -> "$bookName ${r.chapter}:${r.verse}"
    }

    /** Text of the range from the loaded chapters (chapter number -> verses); empty verses skipped. */
    fun text(chapters: Map<Int, List<Verse>>, r: ResolvedRef): String =
        (r.chapter..r.endChapter).flatMap { c ->
            chapters[c].orEmpty().filter { v ->
                !v.isEmpty &&
                    (c != r.chapter || v.verse >= r.verse) &&
                    (c != r.endChapter || v.verse <= r.endVerse)
            }
        }.joinToString(" ") { it.text }

    /** Source keys for a selection: every covered number (a combined verse counts as all of them). */
    fun sourceKeys(book: Int, chapter: Int, verses: Collection<Int>): List<Int> =
        verses.distinct().map { CrossRefParser.key(book, chapter, it) }
}
