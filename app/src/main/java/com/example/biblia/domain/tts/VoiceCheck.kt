package com.example.biblia.domain.tts

/** Per-language readiness of the phone's text-to-speech, as shown in Settings. */
enum class VoiceStatus { INSTALLED_OFFLINE, NEEDS_DOWNLOAD, ENGINE_MISSING }

/** A language that can't be read aloud right now, and why (drives the help dialog). */
data class VoiceProblem(val language: String, val status: VoiceStatus)

/**
 * Pure offline-voice rules. "Listen" only ever uses voices that are installed
 * and need no network connection: an online-only voice would silently fail
 * (or use mobile data) in an app that promises to work offline.
 */
object VoiceCheck {

    /** Installed, network-free voices for [language], best first (quality, then latency). */
    fun offlineVoices(all: List<TtsVoice>, language: String): List<TtsVoice> {
        val codes = TtsLocales.languageCodes(language)
        return all
            .filter { !it.networkRequired && it.installed && it.languageCode in codes }
            .sortedWith(compareByDescending<TtsVoice> { it.quality }.thenBy { it.latency }.thenBy { it.name })
    }

    /** The user's [preferred] voice if it is offline-usable, else the best offline one. */
    fun choose(offline: List<TtsVoice>, preferred: String?): String? =
        offline.firstOrNull { it.name == preferred }?.name ?: offline.firstOrNull()?.name

    /**
     * [voices] null = the engine can't list voices (very old engines); then the
     * language check alone decides.
     */
    fun status(engineReady: Boolean, languageAvailable: Boolean, voices: List<TtsVoice>?, language: String): VoiceStatus =
        when {
            !engineReady -> VoiceStatus.ENGINE_MISSING
            !languageAvailable -> VoiceStatus.NEEDS_DOWNLOAD
            voices == null -> VoiceStatus.INSTALLED_OFFLINE
            offlineVoices(voices, language).isEmpty() -> VoiceStatus.NEEDS_DOWNLOAD
            else -> VoiceStatus.INSTALLED_OFFLINE
        }
}
