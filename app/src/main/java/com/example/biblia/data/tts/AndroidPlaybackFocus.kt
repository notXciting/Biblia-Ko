package com.example.biblia.data.tts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.biblia.domain.tts.PlaybackFocus

/**
 * Audio focus for "Listen" plus ACTION_AUDIO_BECOMING_NOISY (headphones
 * unplugged). Incoming calls take audio focus (transient loss), so reading
 * pauses on a call and resumes after it, without READ_PHONE_STATE.
 */
class AndroidPlaybackFocus(context: Context) : PlaybackFocus {

    private val app = context.applicationContext
    private val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var listener: PlaybackFocus.Listener? = null
    private var request: AudioFocusRequest? = null
    private var receiverRegistered = false

    private val focusChange = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> listener?.onInterrupt(canResume = false)
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> listener?.onInterrupt(canResume = true)
            AudioManager.AUDIOFOCUS_GAIN -> listener?.onResume()
        }
    }

    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                listener?.onInterrupt(canResume = false)
            }
        }
    }

    override fun start(listener: PlaybackFocus.Listener): Boolean {
        this.listener = listener
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val r = request ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                // Speech can't usefully duck: pause instead.
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(focusChange)
                .build()
                .also { request = it }
            audio.requestAudioFocus(r)
        } else {
            @Suppress("DEPRECATION")
            audio.requestAudioFocus(focusChange, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false
        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                app, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            receiverRegistered = true
        }
        return true
    }

    override fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            request?.let { audio.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audio.abandonAudioFocus(focusChange)
        }
        if (receiverRegistered) {
            runCatching { app.unregisterReceiver(noisy) }
            receiverRegistered = false
        }
    }
}
