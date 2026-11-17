package com.example.biblia

import com.example.biblia.domain.reference.ReferenceFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class ReferenceFormatterTest {

    @Test
    fun singleVerse() = assertEquals("Juan 3:16", ReferenceFormatter.format("Juan", 3, listOf(16)))

    @Test
    fun contiguousRange() = assertEquals("Juan 3:16-18", ReferenceFormatter.format("Juan", 3, setOf(18, 16, 17)))

    @Test
    fun mixedRunsAreCompressed() =
        assertEquals("Awit 23:1-3, 5, 7-8", ReferenceFormatter.format("Awit", 23, listOf(8, 1, 2, 3, 5, 7)))

    @Test
    fun duplicatesIgnoredAndEmptyIsChapter() {
        assertEquals("1-2", ReferenceFormatter.ranges(sortedSetOf(1, 2)))
        assertEquals("Juan 3", ReferenceFormatter.format("Juan", 3, emptyList()))
    }
}
