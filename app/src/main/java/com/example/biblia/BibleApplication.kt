package com.example.biblia

import android.app.Application
import com.example.biblia.data.AppIconManager
import com.example.biblia.di.AppContainer
import kotlinx.coroutines.launch

/** Application entry point owning the manual DI container. */
class BibleApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        warmUp()
    }

    /**
     * Opens Room and loads the last-read chapter into the shared cache on IO
     * while the activity is still inflating, so the reader's first frame
     * already has text. Skipped silently before the first import.
     */
    private fun warmUp() {
        val c = container
        c.applicationScope.launch {
            runCatching {
                val s = c.settingsRepository.initial()
                // Keep the launcher alias in sync with the saved icon choice.
                AppIconManager.apply(this@BibleApplication, s.appIcon)
                if (c.settingsRepository.isImported(s.versionId)) {
                    c.chapterCache.get(s.versionId, s.book, s.chapter.coerceAtLeast(1))
                }
            }
        }
    }
}
