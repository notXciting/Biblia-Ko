package com.example.biblia.perf

import android.os.SystemClock
import android.util.Log
import com.example.biblia.BuildConfig

/**
 * VERSE_PERF timing for the jump-to-verse flow: t0 is marked at the tap in the
 * selection screen; every later stage logs elapsed milliseconds. Debug builds
 * only - a no-op in release.
 */
object VersePerf {

    private const val TAG = "VERSE_PERF"

    @Volatile
    private var t0 = 0L

    /** Marks t0 (called at the tap in the selection screen). */
    fun markTap() {
        t0 = SystemClock.elapsedRealtime()
        if (BuildConfig.DEBUG) Log.d(TAG, "tap (t0)")
    }

    /** Logs ms since t0 for a named stage; silent in release. */
    fun stage(name: String) {
        if (!BuildConfig.DEBUG || t0 == 0L) return
        Log.d(TAG, name + ": +" + (SystemClock.elapsedRealtime() - t0) + " ms")
    }
}
