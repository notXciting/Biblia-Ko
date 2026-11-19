package com.example.biblia.domain.tts

import com.example.biblia.domain.model.Verse
import java.util.Locale

/**
 * The speech engine seen by [TtsController]. The Android implementation wraps
 * `TextToSpeech`; JVM tests use a fake. Listener callbacks may arrive on any
 * thread (the controller re-posts them to its own scope).
 */
interface TtsEngine {
    fun isLanguageAvailable(locale: Locale): Boolean

    /** Speaks [text], replacing whatever is being spoken (QUEUE_FLUSH). */
    fun speak(text: String, utteranceId: String, locale: Locale, voiceName: String?, rate: Float, pitch: Float)

    fun stop()

    /** Every voice the engine reports (online ones too), or null if it can't list voices. */
    fun voices(): List<TtsVoice>?

    fun shutdown()

    interface Listener {
        fun onInit(success: Boolean)
        fun onDone(utteranceId: String)
        fun onError(utteranceId: String)
    }
}

/**
 * A voice as reported by the engine. [label] is what the voice picker shows;
 * [languageCode] is the ISO code ("fil", "tl", "en"). Higher [quality] is
 * better, lower [latency] is faster. Filtering lives in [VoiceCheck].
 */
data class TtsVoice(
    val name: String,
    val label: String,
    val languageCode: String,
    val networkRequired: Boolean = false,
    val installed: Boolean = true,
    val quality: Int = 300,
    val latency: Int = 300,
)

/**
 * Audio focus + "becoming noisy" (headphones unplugged). [start] returns false
 * when focus is denied (e.g. during a call).
 */
interface PlaybackFocus {
    fun start(listener: Listener): Boolean
    fun stop()

    interface Listener {
        /** Something else needs the audio. [canResume]: focus will come back. */
        fun onInterrupt(canResume: Boolean)
        fun onResume()
    }
}

/** Verses of a chapter (empty verses included; the controller skips them). */
fun interface ChapterTextSource {
    suspend fun verses(versionId: String, book: Int, chapter: Int): List<Verse>
}

/** Listen settings, persisted in DataStore through SettingsRepository. */
data class TtsPrefs(
    val rate: Float = 1f,
    val pitch: Float = 1f,
    /** language ("Tagalog"/"English") -> voice name; missing = engine default. */
    val voices: Map<String, String> = emptyMap(),
    val speakVerseNumbers: Boolean = false,
) {
    companion object {
        const val MIN_RATE = 0.75f
        const val MAX_RATE = 1.5f
        const val MIN_PITCH = 0.75f
        const val MAX_PITCH = 1.25f
    }
}

enum class TtsStatus { IDLE, PLAYING, PAUSED }

/** What the reader shows: the verse being spoken and the transport state. */
data class TtsState(
    val status: TtsStatus = TtsStatus.IDLE,
    val versionId: String = "",
    val book: Int = 0,
    val chapter: Int = 0,
    val verse: Int = 0,
    /** Last verse the spoken verse covers (combined verses, e.g. 1 -> 2). */
    val verseEnd: Int = verse,
    /** A language with no usable offline voice (drives the help dialog). */
    val voiceProblem: VoiceProblem? = null,
) {
    val isActive: Boolean get() = status != TtsStatus.IDLE
}
