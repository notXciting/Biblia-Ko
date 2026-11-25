package com.example.biblia

import com.example.biblia.domain.crossref.CrossRefParser
import com.example.biblia.domain.crossref.CrossRefResolver
import com.example.biblia.domain.crossref.CrossRefTarget
import com.example.biblia.domain.crossref.ResolvedRef
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseSpans
import org.junit.Assert.assertEquals
import org.junit.Test

/** Cross-reference targets fitted onto versions that merge verses (MBB12: Ruth 1:1 holds 1-2). */
class CrossRefResolverTest {

    // Ruth 1 in MBB12: 1-2 combined, 3, 4, then 5-7 combined.
    private val ruth1 = listOf("Nang ang Israel...", "", "Namatay...", "Nag-asawa...", "Namatay rin...", "", "")
        .mapIndexed { i, t -> Verse(8, 1, i + 1, t) }
    private val ends = VerseSpans.ends(ruth1)
    private val kjvRuth1 = (1..7).map { Verse(8, 1, it, "KJV $it") }

    private fun resolve(t: CrossRefTarget, e: Map<Int, Int> = ends) = CrossRefResolver.resolve(t, e, e)

    @Test
    fun targetOnAMergedNumberResolvesToTheCombinedVerse() {
        val r = resolve(CrossRefTarget(8, 1, 2))
        assertEquals(ResolvedRef(8, 1, 1, 1, 2), r)
        assertEquals("Ruth 1:1-2", CrossRefResolver.label("Ruth", r))
        assertEquals("Nang ang Israel...", CrossRefResolver.text(mapOf(1 to ruth1), r))
        // The first number of a combined verse widens the same way.
        assertEquals(ResolvedRef(8, 1, 1, 1, 2), resolve(CrossRefTarget(8, 1, 1)))
    }

    @Test
    fun rangesWidenToWholeCombinedVerses() {
        // Ruth 1:2-5 -> starts at 1 (holds 2), ends at 7 (5 holds 5-7).
        val r = resolve(CrossRefTarget(8, 1, 2, 1, 5))
        assertEquals(ResolvedRef(8, 1, 1, 1, 7), r)
        assertEquals("Ruth 1:1-7", CrossRefResolver.label("Ruth", r))
        assertEquals("Nang ang Israel... Namatay... Nag-asawa... Namatay rin...", CrossRefResolver.text(mapOf(1 to ruth1), r))
        // A plain verse stays as it is.
        assertEquals("Ruth 1:3", CrossRefResolver.label("Ruth", resolve(CrossRefTarget(8, 1, 3))))
    }

    @Test
    fun versionsWithoutMergedVersesKeepTheTargetAsIs() {
        val r = resolve(CrossRefTarget(8, 1, 2, 1, 4), VerseSpans.ends(kjvRuth1))
        assertEquals(ResolvedRef(8, 1, 2, 1, 4), r)
        assertEquals("KJV 2 KJV 3 KJV 4", CrossRefResolver.text(mapOf(1 to kjvRuth1), r))
    }

    @Test
    fun rangesAcrossAChapter() {
        val ch1 = (29..31).map { Verse(1, 1, it, "1:$it") }
        val ch2 = (1..4).map { Verse(1, 2, it, "2:$it") }
        val r = CrossRefResolver.resolve(CrossRefTarget(1, 1, 31, 2, 3), emptyMap(), emptyMap())
        assertEquals("Genesis 1:31-2:3", CrossRefResolver.label("Genesis", r))
        assertEquals("1:31 2:1 2:2 2:3", CrossRefResolver.text(mapOf(1 to ch1, 2 to ch2), r))
    }

    @Test
    fun aCombinedSourceVerseLooksUpAllItsNumbers() {
        val covered = VerseSpans.covered(setOf(1), ends) // tapping "1-2" selects 1 and 2
        assertEquals(
            listOf(CrossRefParser.key(8, 1, 1), CrossRefParser.key(8, 1, 2)),
            CrossRefResolver.sourceKeys(8, 1, covered),
        )
    }
}
