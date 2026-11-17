package com.example.biblia.di

import android.util.Log
import com.example.biblia.BuildConfig
import com.example.biblia.perf.VersePerf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One "open this verse in the reader" request. verse == 0 means chapter only. */
data class OpenVerseRequest(
    val versionId: String,
    val book: Int,
    val chapter: Int,
    val verse: Int,
)

/**
 * Holds the PENDING navigation request in a StateFlow so it is never lost if
 * the reader is not collecting at that moment (StateFlow replays the current
 * value on subscribe). One instance lives in AppContainer and is shared by the
 * reader and selection ViewModels.
 */
class NavigationBus {

    private val _request = MutableStateFlow<OpenVerseRequest?>(null)
    val request: StateFlow<OpenVerseRequest?> = _request.asStateFlow()

    fun open(request: OpenVerseRequest) {
        VersePerf.stage("bus publish")
        if (BuildConfig.DEBUG) Log.d(VERSE_NAV_TAG, "Bus published: " + request)
        _request.value = request
    }

    fun consume() {
        _request.value = null
    }

    companion object {
        const val VERSE_NAV_TAG = "VERSE_NAV"
    }
}
