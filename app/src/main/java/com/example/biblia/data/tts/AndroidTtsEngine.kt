package com.example.biblia.data.tts

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.biblia.domain.tts.TtsEngine
import com.example.biblia.domain.tts.TtsVoice
import java.util.Locale

/**
 * [TtsEngine] over Android's built-in TextToSpeech (on-device engine, no
 * network or permission). Locale/voice are only re-applied when they change.
 */
class AndroidTtsEngine(context: Context, private val listener: TtsEngine.Listener) : TtsEngine {

    private val tts = TextToSpeech(context.applicationContext) { status ->
        // SUCCESS can be reported with no engine bound on some ROMs: re-check.
        listener.onInit(status == TextToSpeech.SUCCESS && hasEngine())
    }
    private var appliedLocale: Locale? = null
    private var appliedVoice: String? = null

    init {
        // No TTS engine on the phone at all: report it now instead of waiting.
        if (!hasEngine()) listener.onInit(false)
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { utteranceId?.let(listener::onDone) }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { utteranceId?.let(listener::onError) }
        })
    }

    override fun isLanguageAvailable(locale: Locale): Boolean =
        tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE

    override fun speak(text: String, utteranceId: String, locale: Locale, voiceName: String?, rate: Float, pitch: Float) {
        if (locale != appliedLocale || voiceName != appliedVoice) {
            tts.language = locale
            if (voiceName != null) {
                runCatching { tts.voices }.getOrNull()?.firstOrNull { it.name == voiceName }?.let { tts.voice = it }
            }
            appliedLocale = locale
            appliedVoice = voiceName
        }
        tts.setSpeechRate(rate)
        tts.setPitch(pitch)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun stop() { tts.stop() }

    /** Raw voice facts; [com.example.biblia.domain.tts.VoiceCheck] decides what is offline-usable. */
    override fun voices(): List<TtsVoice>? =
        runCatching { tts.voices }.getOrNull()?.map { voice ->
            TtsVoice(
                name = voice.name,
                label = label(voice),
                languageCode = voice.locale.language,
                networkRequired = voice.isNetworkConnectionRequired,
                installed = TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features.orEmpty(),
                quality = voice.quality,
                latency = voice.latency,
            )
        }

    private fun hasEngine(): Boolean = runCatching { tts.engines.isNotEmpty() }.getOrDefault(false)

    override fun shutdown() { tts.shutdown() }

    /** "English (United States) · iol" from "en-us-x-iol-local". */
    private fun label(voice: Voice): String {
        val place = voice.locale.getDisplayName(voice.locale)
        val id = voice.name.substringAfter("-x-", "").substringBefore('-').ifBlank { voice.name }
        return "$place · $id"
    }
}
