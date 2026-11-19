package com.example.biblia

import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.tts.ChapterTextSource
import com.example.biblia.domain.tts.PlaybackFocus
import com.example.biblia.domain.tts.TtsController
import com.example.biblia.domain.tts.TtsEngine
import com.example.biblia.domain.tts.TtsPrefs
import com.example.biblia.domain.tts.TtsStatus
import com.example.biblia.domain.tts.TtsVoice
import com.example.biblia.domain.tts.VoiceCheck
import com.example.biblia.domain.tts.VoiceProblem
import com.example.biblia.domain.tts.VoiceStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class TtsControllerTest {

    private companion object {
        fun voice(name: String, lang: String, network: Boolean = false, installed: Boolean = true, quality: Int = 300, latency: Int = 300) =
            TtsVoice(name, name, lang, network, installed, quality, latency)

        val OFFLINE_VOICES = listOf(voice("fil-a", "fil"), voice("en-a", "en"))
    }

    private class Spoken(val text: String, val id: String, val locale: Locale, val voice: String?)

    private class FakeEngine(
        val listener: TtsEngine.Listener,
        val missing: Set<String>,
        val voiceList: List<TtsVoice>?,
        initOk: Boolean,
    ) : TtsEngine {
        val spoken = mutableListOf<Spoken>()
        var stops = 0
        init { listener.onInit(initOk) }
        override fun isLanguageAvailable(locale: Locale) = locale.language !in missing
        override fun speak(text: String, utteranceId: String, locale: Locale, voiceName: String?, rate: Float, pitch: Float) {
            spoken += Spoken(text, utteranceId, locale, voiceName)
        }
        override fun stop() { stops++ }
        override fun voices() = voiceList
        override fun shutdown() {}
        fun finishLast() = listener.onDone(spoken.last().id)
    }

    private class FakeFocus : PlaybackFocus {
        var held = false
        override fun start(listener: PlaybackFocus.Listener): Boolean { held = true; return true }
        override fun stop() { held = false }
    }

    private val mbb = BibleVersionRegistry.byId("mbb12")
    private val kjv = BibleVersionRegistry.byId("kjv")

    /** Every chapter has 3 verses "<version> b:c:v" unless overridden. */
    private val overrides = mutableMapOf<String, List<Verse>>()
    private val source = ChapterTextSource { v, b, c ->
        overrides["$v $b:$c"] ?: (1..3).map { Verse(b, c, it, "$v $b:$c:$it") }
    }

    private lateinit var engine: FakeEngine
    private val focus = FakeFocus()

    private fun TestScope.controller(
        missing: Set<String> = emptySet(),
        voices: List<TtsVoice>? = OFFLINE_VOICES,
        initOk: Boolean = true,
    ) = TtsController({ l -> FakeEngine(l, missing, voices, initOk).also { engine = it } }, focus, source, this)

    private fun TestScope.finishAndAdvance(times: Int = 1) = repeat(times) {
        engine.finishLast()
        advanceUntilIdle()
    }

    private fun texts() = engine.spoken.map { it.text }

    @Test
    fun readsVersesInOrderFromTheStartVerse() = runTest {
        val tts = controller()
        tts.play(mbb, 43, 3, 2)
        advanceUntilIdle()
        finishAndAdvance(1)
        assertEquals(listOf("mbb12 43:3:2", "mbb12 43:3:3"), texts())
        assertEquals(TtsStatus.PLAYING, tts.state.value.status)
        assertEquals(3, tts.state.value.verse)
        assertTrue(focus.held)
    }

    @Test
    fun skipsEmptyVerses() = runTest {
        overrides["mbb12 1:1"] = listOf(Verse(1, 1, 1, "a"), Verse(1, 1, 2, "  "), Verse(1, 1, 3, "c"))
        val tts = controller()
        tts.play(mbb, 1, 1, 1)
        advanceUntilIdle()
        finishAndAdvance(1)
        assertEquals(listOf("a", "c"), texts())
        // Verse 2 is merged into verse 1 ("1-2"): starting on 2 reads 1-2.
        tts.play(mbb, 1, 1, 2)
        advanceUntilIdle()
        assertEquals("a", texts().last())
        assertEquals(1 to 2, tts.state.value.verse to tts.state.value.verseEnd)
        tts.updatePrefs(TtsPrefs(speakVerseNumbers = true))
        tts.play(mbb, 1, 1, 1)
        advanceUntilIdle()
        assertEquals("1-2. a", texts().last())
    }

    @Test
    fun rollsOverIntoNextChapterAndNextBook() = runTest {
        val tts = controller()
        tts.play(mbb, 1, 49, 3) // Genesis 49:3 (last verse of the fake chapter)
        advanceUntilIdle()
        finishAndAdvance(1)
        assertEquals("mbb12 1:50:1", texts().last())
        finishAndAdvance(3) // through Genesis 50 into Exodus 1
        assertEquals("mbb12 2:1:1", texts().last())
        assertEquals(2 to 1, tts.state.value.book to tts.state.value.chapter)
    }

    @Test
    fun skipsChaptersWithoutTextAndStopsAfterRevelation() = runTest {
        overrides["mbb12 66:21"] = emptyList()
        val tts = controller()
        tts.play(mbb, 66, 20, 3)
        advanceUntilIdle()
        finishAndAdvance(1)
        assertEquals("mbb12 66:22:1", texts().last())
        finishAndAdvance(3)
        assertEquals(TtsStatus.IDLE, tts.state.value.status)
        assertTrue(!focus.held)
    }

    @Test
    fun versionSwitchRestartsSameVerseInNewLanguage() = runTest {
        val tts = controller()
        tts.play(mbb, 43, 3, 2)
        advanceUntilIdle()
        assertEquals("fil", engine.spoken.last().locale.language)
        val stopsBefore = engine.stops
        tts.onVersionChanged(kjv)
        advanceUntilIdle()
        assertTrue(engine.stops > stopsBefore)
        assertEquals("kjv 43:3:2", texts().last())
        assertEquals(Locale.US, engine.spoken.last().locale)
        assertEquals("kjv", tts.state.value.versionId)
        // A late "done" for the flushed Tagalog utterance must not advance.
        engine.listener.onDone(engine.spoken[0].id)
        advanceUntilIdle()
        assertEquals("kjv 43:3:2", texts().last())
    }

    @Test
    fun missingVoiceFlagsOnlyThatLanguage() = runTest {
        val tts = controller(missing = setOf("fil", "tl"))
        tts.play(mbb, 1, 1, 1)
        advanceUntilIdle()
        assertEquals(VoiceProblem("Tagalog", VoiceStatus.NEEDS_DOWNLOAD), tts.state.value.voiceProblem)
        assertTrue(engine.spoken.isEmpty())
        tts.dismissVoiceProblem()
        tts.play(kjv, 1, 1, 1)
        advanceUntilIdle()
        assertNull(tts.state.value.voiceProblem)
        assertEquals("kjv 1:1:1", texts().last())
        // Switching to the Tagalog version while playing stops with the dialog.
        tts.onVersionChanged(mbb)
        advanceUntilIdle()
        assertEquals(TtsStatus.IDLE, tts.state.value.status)
        assertEquals("Tagalog", tts.state.value.voiceProblem?.language)
    }

    @Test
    fun onlyNetworkVoicesCountAsNotInstalled() = runTest {
        val tts = controller(voices = listOf(voice("fil-net", "fil", network = true), voice("en-a", "en")))
        assertEquals(VoiceStatus.NEEDS_DOWNLOAD, tts.voiceStatus("Tagalog"))
        assertEquals(VoiceStatus.INSTALLED_OFFLINE, tts.voiceStatus("English"))
        tts.play(mbb, 1, 1, 1)
        advanceUntilIdle()
        assertEquals(VoiceProblem("Tagalog", VoiceStatus.NEEDS_DOWNLOAD), tts.state.value.voiceProblem)
        assertTrue(engine.spoken.isEmpty())
        assertTrue(tts.voices("Tagalog").isEmpty())
    }

    @Test
    fun noEngineAtAllIsReportedAsEngineMissing() = runTest {
        val tts = controller(initOk = false)
        assertEquals(VoiceStatus.ENGINE_MISSING, tts.voiceStatus("English"))
        tts.play(kjv, 1, 1, 1)
        advanceUntilIdle()
        assertEquals(VoiceProblem("English", VoiceStatus.ENGINE_MISSING), tts.state.value.voiceProblem)
        assertEquals(TtsStatus.IDLE, tts.state.value.status)
    }

    @Test
    fun speaksWithAnOfflineVoiceNeverANetworkOne() = runTest {
        val voices = listOf(
            voice("en-net", "en", network = true, quality = 500),
            voice("en-low", "en", quality = 200),
            voice("en-best", "en", quality = 400),
        )
        val tts = controller(voices = voices)
        tts.updatePrefs(TtsPrefs(voices = mapOf("English" to "en-net"))) // stale online choice
        tts.play(kjv, 1, 1, 1)
        advanceUntilIdle()
        assertEquals("en-best", engine.spoken.last().voice)
        tts.updatePrefs(TtsPrefs(voices = mapOf("English" to "en-low")))
        finishAndAdvance(1)
        assertEquals("en-low", engine.spoken.last().voice) // applies on the next verse
    }

    @Test
    fun voiceCheckFiltersAndRanksOfflineVoices() {
        val all = listOf(
            voice("tl-net", "tl", network = true),
            voice("fil-missing", "fil", installed = false),
            voice("fil-ok", "fil", quality = 300, latency = 400),
            voice("fil-fast", "fil", quality = 300, latency = 100),
            voice("en-x", "en"),
        )
        assertEquals(listOf("fil-fast", "fil-ok"), VoiceCheck.offlineVoices(all, "Tagalog").map { it.name })
        assertEquals(VoiceStatus.ENGINE_MISSING, VoiceCheck.status(false, true, all, "English"))
        assertEquals(VoiceStatus.NEEDS_DOWNLOAD, VoiceCheck.status(true, false, all, "English"))
        // Engines that can't list voices: the language check alone decides.
        assertEquals(VoiceStatus.INSTALLED_OFFLINE, VoiceCheck.status(true, true, null, "Tagalog"))
        assertEquals(VoiceStatus.NEEDS_DOWNLOAD, VoiceCheck.status(true, true, listOf(all[0]), "Tagalog"))
    }

    @Test
    fun nextPreviousPauseResumeAndVerseNumbers() = runTest {
        val tts = controller()
        tts.updatePrefs(TtsPrefs(speakVerseNumbers = true))
        tts.play(kjv, 2, 1, 1)
        advanceUntilIdle()
        assertEquals("1. kjv 2:1:1", texts().last())
        tts.previous() // Exodus 1:1 -> Genesis 50:3
        advanceUntilIdle()
        assertEquals("3. kjv 1:50:3", texts().last())
        tts.next()
        advanceUntilIdle()
        assertEquals("1. kjv 2:1:1", texts().last())
        tts.pause()
        assertEquals(TtsStatus.PAUSED, tts.state.value.status)
        engine.finishLast()
        advanceUntilIdle()
        assertEquals(3, engine.spoken.size) // paused: no advance
        tts.resume()
        advanceUntilIdle()
        assertEquals("1. kjv 2:1:1", texts().last())
        tts.stop()
        assertEquals(TtsStatus.IDLE, tts.state.value.status)
    }
}
