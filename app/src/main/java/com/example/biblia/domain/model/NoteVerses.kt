package com.example.biblia.domain.model

/** A note as the UI sees it. [verses] is never empty. */
data class VerseNote(
    val id: Long,
    val book: Int,
    val chapter: Int,
    val verses: Set<Int>,
    val text: String,
    val updatedAt: Long,
)

/** Encoding and matching of a note's verse set (pure, unit-tested). */
object NoteVerses {

    /** {18, 16, 17} -> "16,17,18" (sorted, so equal sets give equal strings). */
    fun encode(verses: Collection<Int>): String = verses.toSortedSet().joinToString(",")

    fun decode(raw: String): Set<Int> =
        raw.split(',').mapNotNullTo(sortedSetOf()) { it.trim().toIntOrNull()?.takeIf { n -> n > 0 } }

    /** The note for exactly this verse selection, if any (selecting 16-17 again edits it). */
    fun exact(notes: List<VerseNote>, verses: Set<Int>): VerseNote? = notes.firstOrNull { it.verses == verses }

    /**
     * Where each note's marker goes in the reader: after the LAST verse it
     * covers, mapped to the text verse that shows it (combined verses: a note
     * on verse 2 of "1-2" is marked on 1). verse -> note ids, oldest first.
     */
    fun markers(notes: List<VerseNote>, spanEnds: Map<Int, Int>): Map<Int, List<Long>> =
        notes.groupBy({ VerseSpans.ownerOf(it.verses.max(), spanEnds) }, { it.id })
}
