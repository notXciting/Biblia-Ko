package com.example.biblia.domain.tts

import com.example.biblia.data.BibleVersion
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseSpans
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * "Listen": reads verse by verse (one utterance per verse, empty verses
 * skipped) and rolls over into the next chapter and book until stopped. The
 * voice follows the version's language. All state lives on [scope] (Main in
 * the app); engine callbacks are re-posted there and matched by utterance id,
 * so a stale "done" from a flushed utterance never advances the queue.
 */
class TtsController(
    private val engineFactory: (TtsEngine.Listener) -> TtsEngine,
    private val focus: PlaybackFocus,
    private val source: ChapterTextSource,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(TtsState())
    val state: StateFlow<TtsState> = _state

    private var engine: TtsEngine? = null
    private var engineReady = CompletableDeferred<Boolean>()
    private var prefs = TtsPrefs()
    private var version: BibleVersion? = null
    private var locale: Locale = Locale.US
    /** Offline voices of the current version's language (null = engine can't list them). */
    private var offline: List<TtsVoice>? = null
    private var book = 0
    private var chapter = 0
    private var verses: List<Verse> = emptyList() // non-empty verses only
    private var spanEnds: Map<Int, Int> = emptyMap() // combined verses ("1-2")
    private var index = 0
    private var utterance = 0
    private var currentId: String? = null
    private var errors = 0
    private var resumeOnFocus = false
    private var job: Job? = null

    private val status: TtsStatus get() = _state.value.status

    private val engineListener = object : TtsEngine.Listener {
        override fun onInit(success: Boolean) { engineReady.complete(success) }
        override fun onDone(utteranceId: String) { scope.launch { onFinished(utteranceId, ok = true) } }
        override fun onError(utteranceId: String) { scope.launch { onFinished(utteranceId, ok = false) } }
    }

    private val focusListener = object : PlaybackFocus.Listener {
        override fun onInterrupt(canResume: Boolean) {
            scope.launch {
                if (status != TtsStatus.PLAYING) return@launch
                pause(keepFocus = canResume)
                resumeOnFocus = canResume
            }
        }
        override fun onResume() {
            scope.launch { if (resumeOnFocus && status == TtsStatus.PAUSED) resume() }
        }
    }

    fun updatePrefs(value: TtsPrefs) { prefs = value }

    /** Starts at [verse] (or the next non-empty one) of [book]:[chapter]. */
    fun play(version: BibleVersion, book: Int, chapter: Int, verse: Int): Unit = command {
        silence()
        val voice = resolve(version.language) ?: return@command
        if (!focus.start(focusListener)) { halt(); return@command }
        this.version = version
        locale = voice.first
        offline = voice.second
        if (load(book, chapter, verse = verse)) speakCurrent() else halt()
    }

    fun pause(): Unit = pause(keepFocus = false)

    fun resume(): Unit = command {
        if (status != TtsStatus.PAUSED) return@command
        resumeOnFocus = false
        if (focus.start(focusListener)) speakCurrent()
    }

    fun stop() {
        job?.cancel()
        halt()
    }

    /** Stop without cancelling [job] (safe to call from inside a command). */
    private fun halt() {
        silence()
        resumeOnFocus = false
        focus.stop()
        _state.value = TtsState(voiceProblem = _state.value.voiceProblem)
    }

    fun next(): Unit = command {
        if (index < verses.lastIndex) index++
        else ChapterSteps.next(book, chapter)?.let { (b, c) -> if (!load(b, c)) return@command } ?: return@command
        continueAt()
    }

    fun previous(): Unit = command {
        if (index > 0) index--
        else ChapterSteps.previous(book, chapter)?.let { (b, c) -> if (!load(b, c, atEnd = true)) return@command }
            ?: return@command
        continueAt()
    }

    /** Version switched in the reader: restart from the current verse in the new voice. */
    fun onVersionChanged(newVersion: BibleVersion) {
        if (!_state.value.isActive || newVersion.id == version?.id) return
        command {
            val wasPlaying = status == TtsStatus.PLAYING
            val currentVerse = verses.getOrNull(index)?.verse ?: 1
            silence()
            val voice = resolve(newVersion.language) ?: return@command
            version = newVersion
            locale = voice.first
            offline = voice.second
            if (!load(book, chapter, verse = currentVerse)) { halt(); return@command }
            if (wasPlaying) speakCurrent() else publish(TtsStatus.PAUSED)
        }
    }

    /** Speaks a sample with the current settings (pauses reading first). */
    fun testVoice(language: String, text: String): Unit = command {
        pauseNow(keepFocus = false)
        val (loc, voices) = resolve(language) ?: return@command
        engine?.speak(text, "test", loc, pickVoice(voices, language), prefs.rate, prefs.pitch)
    }

    /** Installed offline / needs download / no engine, for [language]. */
    suspend fun voiceStatus(language: String): VoiceStatus {
        val e = awaitEngine() ?: return VoiceStatus.ENGINE_MISSING
        val available = TtsLocales.candidates(language).any { e.isLanguageAvailable(it) }
        return VoiceCheck.status(true, available, e.voices(), language)
    }

    /** Offline voices only, best first (the Settings voice picker). */
    suspend fun voices(language: String): List<TtsVoice> =
        VoiceCheck.offlineVoices(awaitEngine()?.voices().orEmpty(), language)

    /** Opens the help dialog for [language] (e.g. from the Settings status row). */
    fun showVoiceHelp(language: String, status: VoiceStatus) {
        _state.value = _state.value.copy(voiceProblem = VoiceProblem(language, status))
    }

    fun dismissVoiceProblem() { _state.value = _state.value.copy(voiceProblem = null) }

    /** App is going away: stop and free the engine (re-created on next use). */
    fun release() {
        stop()
        engine?.shutdown()
        engine = null
    }

    private fun command(block: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch { block() }
    }

    private fun pause(keepFocus: Boolean) {
        if (status != TtsStatus.PLAYING) return
        job?.cancel()
        pauseNow(keepFocus)
    }

    private fun pauseNow(keepFocus: Boolean) {
        if (status != TtsStatus.PLAYING) return
        silence()
        if (!keepFocus) focus.stop()
        publish(TtsStatus.PAUSED)
    }

    private fun silence() {
        currentId = null
        engine?.stop()
    }

    private fun continueAt() {
        if (status == TtsStatus.PLAYING) speakCurrent() else publish(status)
    }

    private fun onFinished(id: String, ok: Boolean) {
        if (id != currentId || status != TtsStatus.PLAYING) return
        errors = if (ok) 0 else errors + 1
        if (errors >= MAX_ERRORS) { stop(); return }
        command {
            if (index < verses.lastIndex) { index++; speakCurrent(); return@command }
            val next = ChapterSteps.next(book, chapter)
            if (next != null && load(next.first, next.second)) speakCurrent() else halt()
        }
    }

    private fun speakCurrent() {
        val e = engine ?: return
        val v = version ?: return
        val verse = verses.getOrNull(index) ?: return
        val id = "verse-" + ++utterance
        currentId = id
        publish(TtsStatus.PLAYING)
        val text = if (prefs.speakVerseNumbers) VerseSpans.label(verse.verse, spanEnds) + ". " + verse.text else verse.text
        e.speak(text, id, locale, pickVoice(offline, v.language), prefs.rate, prefs.pitch)
    }

    private fun publish(status: TtsStatus) {
        val verse = verses.getOrNull(index)?.verse ?: 0
        _state.value = TtsState(status, version?.id.orEmpty(), book, chapter, verse, VerseSpans.end(verse, spanEnds))
    }

    /**
     * Loads [b]:[c], stepping on past chapters with no text. Positions at the
     * first verse >= [verse], or at the last verse when [atEnd].
     */
    private suspend fun load(b: Int, c: Int, atEnd: Boolean = false, verse: Int = 1): Boolean {
        val versionId = version?.id ?: return false
        var pos: Pair<Int, Int>? = b to c
        repeat(MAX_EMPTY_CHAPTERS) {
            val (pb, pc) = pos ?: return false
            val raw = runCatching { source.verses(versionId, pb, pc) }.getOrDefault(emptyList())
            val list = raw.filter { !it.isEmpty }
            if (list.isNotEmpty()) {
                val ends = VerseSpans.ends(raw)
                book = pb; chapter = pc; verses = list; spanEnds = ends
                // Starting on a merged number (2 of "1-2") starts at its combined verse.
                index = if (atEnd) list.lastIndex
                else list.indexOfFirst { VerseSpans.end(it.verse, ends) >= verse }.takeIf { it >= 0 } ?: list.lastIndex
                return true
            }
            pos = if (atEnd) ChapterSteps.previous(pb, pc) else ChapterSteps.next(pb, pc)
        }
        return false
    }

    /** Chosen offline voice, re-read per verse so a Settings change applies on the next verse. */
    private fun pickVoice(voices: List<TtsVoice>?, language: String): String? =
        voices?.let { VoiceCheck.choose(it, prefs.voices[language]) }

    /**
     * Locale + offline voices for [language]; null (after raising a
     * [VoiceProblem]) when there is no engine, the language is missing, or
     * only network voices exist.
     */
    private suspend fun resolve(language: String): Pair<Locale, List<TtsVoice>?>? {
        val e = awaitEngine()
        val loc = e?.let { TtsLocales.candidates(language).firstOrNull { l -> e.isLanguageAvailable(l) } }
        val all = e?.voices()
        val status = VoiceCheck.status(e != null, loc != null, all, language)
        if (loc == null || status != VoiceStatus.INSTALLED_OFFLINE) {
            halt()
            _state.value = TtsState(voiceProblem = VoiceProblem(language, status))
            return null
        }
        return loc to all?.let { VoiceCheck.offlineVoices(it, language) }
    }

    private suspend fun awaitEngine(): TtsEngine? {
        if (engine == null) {
            engineReady = CompletableDeferred()
            engine = engineFactory(engineListener)
        }
        if (engineReady.await()) return engine
        engine?.shutdown()
        engine = null
        return null
    }

    private companion object {
        const val MAX_ERRORS = 3
        const val MAX_EMPTY_CHAPTERS = 8
    }
}
