package com.example.biblia

import android.app.Activity
import android.os.Build
import android.view.Display
import android.view.Surface
import android.view.SurfaceView

/**
 * Requests the highest refresh rate the display supports at the current
 * resolution (60/90/120 Hz) so scrolling renders at full rate. Every step is
 * guarded: this can never crash the app.
 */
fun Activity.requestHighRefreshRate() {
    try {
        val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }
        val current = display?.mode ?: return
        val best = display.supportedModes
            .filter {
                it.physicalWidth == current.physicalWidth &&
                    it.physicalHeight == current.physicalHeight
            }
            .maxByOrNull { it.refreshRate }
            ?: return
        if (best.modeId != current.modeId && best.refreshRate > current.refreshRate + 0.01f) {
            val attributes = window.attributes
            attributes.preferredDisplayModeId = best.modeId
            window.attributes = attributes
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Best effort: hint the frame rate on a SurfaceView-hosted surface if
            // one is present (Compose draws to the window surface, so this is
            // usually a no-op). Guarded so it can never throw.
            runCatching {
                val root = window.decorView.rootView
                if (root is SurfaceView) {
                    root.holder.surface.setFrameRate(
                        best.refreshRate,
                        Surface.FRAME_RATE_COMPATIBILITY_DEFAULT,
                    )
                }
            }
        }
    } catch (t: Throwable) {
        // Ignore: high refresh rate is an enhancement only.
    }
}
