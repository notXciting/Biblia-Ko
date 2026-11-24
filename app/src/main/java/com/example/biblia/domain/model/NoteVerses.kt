package com.example.biblia.domain.model

import com.example.biblia.domain.search.TextNormalizer

/** A note as the UI sees it. [verses] is never empty. */
data class VerseNote(
    val id: Long,
    val book: Int,
    val chapter: Int,
    val verses: Set<Int>,
    val text: String,
    val updatedAt: Long,
    val createdAt: Long = updatedAt,
)

/** Order of the notes list. */
enum class NoteSort { RECENT, BIBLE }

/** Encoding and matching of a note's verse set (pure, unit-tested). */
object NoteVerses {

    /** {18, 16, 17} -> "16,17,18" (sorted, so equal sets give equal strings). */
    fun encode(verses: Collection<Int>): String = verses.toSortedSet().joinToString(",")

    fun decode(raw: String): Set<Int> =
        raw.split(',').mapNotNullTo(sortedSetOf()) { it.trim().toIntOrNull()?.takeIf { n -> n > 0 } }

    /** The note for exactly this verse selection, if any (selecting 16-17 again edits it). */
    fun exact(notes: List<VerseNote>, verses: Set<Int>): VerseNote? = notes.firstOrNull { it.verses == verses }

    /**
     * Text of the verses a note covers, from one version's chapter: a combined
     * verse ("1-2") counts when any number it covers is in [verses].
     */
    fun quote(chapter: List<Verse>, spanEnds: Map<Int, Int>, verses: Set<Int>): String =
        chapter.filter { v -> !v.isEmpty && (v.verse..VerseSpans.end(v.verse, spanEnds)).any { it in verses } }
            .joinToString(" ") { it.text }

    /**
     * The notes list: notes whose text or reference contains every word of
     * [query] (accent/case-insensitive), sorted newest first or in Bible order.
     */
    fun list(notes: List<VerseNote>, query: String, sort: NoteSort, reference: (VerseNote) -> String): List<VerseNote> {
        val words = TextNormalizer.fold(query.lowercase())
            .split(' ').filter { it.isNotBlank() }
        val found = if (words.isEmpty()) notes else notes.filter { note ->
            val haystack = TextNormalizer.fold((note.text + " " + reference(note)).lowercase())
            words.all { it in haystack }
        }
        return when (sort) {
            NoteSort.RECENT -> found.sortedByDescending { it.updatedAt }
            NoteSort.BIBLE -> found.sortedWith(compareBy({ it.book }, { it.chapter }, { it.verses.min() }, { it.createdAt }))
        }
    }

    /**
     * Where each note's marker goes in the reader: after the LAST verse it
     * covers, mapped to the text verse that shows it (combined verses: a note
     * on verse 2 of "1-2" is marked on 1). verse -> note ids, oldest first.
     */
    fun markers(notes: List<VerseNote>, spanEnds: Map<Int, Int>): Map<Int, List<Long>> =
        notes.groupBy({ VerseSpans.ownerOf(it.verses.max(), spanEnds) }, { it.id })
}
