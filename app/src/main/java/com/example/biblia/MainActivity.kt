package com.example.biblia

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.domain.model.BookNameLanguage
import com.example.biblia.ui.AppRoot
import com.example.biblia.ui.LocalizedContent
import com.example.biblia.ui.theme.BibleTheme
import com.example.biblia.ui.uiLocaleTag
import kotlinx.coroutines.runBlocking

/** Single-activity Compose host: settings-driven theme + language + AppRoot shell. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as BibleApplication).container
        // One small DataStore read before the first frame, so the saved theme,
        // UI language and book-name language apply immediately (no flash of
        // the defaults on launch).
        val initial = runBlocking { container.settingsRepository.initial() }
        BookNameLanguage.english = BibleVersionRegistry.byId(initial.versionId).isEnglish
        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = initial)
            val bookNamesEnglish = BibleVersionRegistry.byId(settings.versionId).isEnglish
            SideEffect { BookNameLanguage.english = bookNamesEnglish }
            val systemDark = isSystemInDarkTheme()
            val dark = settings.themeMode.isDark(systemDark)
            // System bar icons follow the APP theme, not just the system one.
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            LocalizedContent(localeTag = uiLocaleTag(settings), uiScale = settings.uiScale) {
                BibleTheme(settings = settings) {
                    AppRoot(container = container)
                }
            }
        }
        // Request 60/90/120 Hz where the display supports it (guarded, never crashes).
        requestHighRefreshRate()
    }

    override fun onDestroy() {
        // Leaving the app (not a rotation): stop "Listen" and free the engine.
        if (isFinishing) (application as BibleApplication).container.tts.release()
        super.onDestroy()
    }
}
