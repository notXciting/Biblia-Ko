package com.example.biblia

import com.example.biblia.di.NavigationBus
import com.example.biblia.di.OpenVerseRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies the selection -> reader navigation seam used by verse taps, chapter
 * taps and search results: a pick publishes an [OpenVerseRequest] with exactly
 * (versionId, book, chapter, verse) on the bus instance owned by AppContainer,
 * and the pending value is held until consumed (so it survives the reader not
 * collecting yet).
 */
class NavigationBusTest {

    @Test
    fun `open publishes the exact verse selection`() {
        val bus = NavigationBus()
        bus.consume()
        bus.open(OpenVerseRequest(versionId = "mbb12", book = 43, chapter = 3, verse = 16))
        val published = bus.request.value
        assertEquals("mbb12", published?.versionId)
        assertEquals(43, published?.book)
        assertEquals(3, published?.chapter)
        assertEquals(16, published?.verse)
    }

    @Test
    fun `pending request is held until consumed`() {
        val bus = NavigationBus()
        bus.open(OpenVerseRequest("mbb12", 1, 1, 1))
        assertEquals(1, bus.request.value?.book)
        bus.consume()
        assertNull(bus.request.value)
    }

    @Test
    fun `chapter-only request carries verse zero`() {
        val bus = NavigationBus()
        bus.consume()
        bus.open(OpenVerseRequest("mbb12", 19, 23, 0))
        assertEquals(0, bus.request.value?.verse)
    }
}
