package com.example.biblia.ui.listen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Toast
import com.example.biblia.R

/** System screens for fixing text-to-speech. Each falls back instead of crashing. */

private const val SPEECH_SERVICES_PACKAGE = "com.google.android.tts"
private const val TTS_SETTINGS_ACTION = "com.android.settings.TTS_SETTINGS"

/** The engine's "Install voice data" screen; the TTS settings if the engine has none. */
fun openVoiceInstaller(context: Context) {
    if (!tryStart(context, Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))) openTtsSettings(context)
}

/** Settings > Text-to-speech output; general Settings as a last resort. */
fun openTtsSettings(context: Context) {
    if (tryStart(context, Intent(TTS_SETTINGS_ACTION))) return
    if (tryStart(context, Intent(Settings.ACTION_SETTINGS))) return
    Toast.makeText(context, R.string.voice_install_unavailable, Toast.LENGTH_LONG).show()
}

/** "Speech Services by Google" in the Play Store app, else in the browser. */
fun openSpeechServicesListing(context: Context) {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$SPEECH_SERVICES_PACKAGE"))
    val web = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$SPEECH_SERVICES_PACKAGE"),
    )
    if (!tryStart(context, market) && !tryStart(context, web)) openTtsSettings(context)
}

private fun tryStart(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (e: ActivityNotFoundException) {
    false
} catch (e: SecurityException) {
    false
}
