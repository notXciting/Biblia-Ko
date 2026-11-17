package com.example.biblia.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biblia.data.AppIconManager
import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.di.AppContainer
import com.example.biblia.domain.model.AccentColor
import com.example.biblia.domain.model.AppIcon
import com.example.biblia.domain.model.AppLanguage
import com.example.biblia.domain.model.LineSpacing
import com.example.biblia.domain.model.ThemeMode
import com.example.biblia.domain.model.Verse
import com.example.biblia.domain.model.VerseLayout
import com.example.biblia.domain.reference.ReferenceFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings state holders that write straight to DataStore. Current values are
 * observed reactively by AppRoot (settings flow), so every change applies live
 * to the whole app.
 */
class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val settingsRepo = container.settingsRepository

    /** Psalm 23:1-2 from the selected version, for the verse-layout preview. */
    var previewVerses by mutableStateOf<List<Verse>>(emptyList()); private set
    var previewReference by mutableStateOf(""); private set

    /** John 3:16 from the selected version for the font-size preview (null = not loaded yet). */
    var fontPreviewText by mutableStateOf<String?>(null); private set
    var fontPreviewReference by mutableStateOf<String?>(null); private set

    init {
        viewModelScope.launch {
            val versionId = settingsRepo.initial().versionId
            val verses = runCatching { container.chapterCache.get(versionId, PREVIEW_BOOK, PREVIEW_CHAPTER) }
                .getOrNull()?.verses?.filter { it.verse in PREVIEW_VERSES && !it.isEmpty }.orEmpty()
            previewReference = ReferenceFormatter.format(
                BookCatalog.book(PREVIEW_BOOK).displayName, PREVIEW_CHAPTER, verses.map { it.verse },
            )
            previewVerses = verses
            val john316 = runCatching { container.chapterCache.get(versionId, JOHN, 3) }
                .getOrNull()?.verses?.firstOrNull { it.verse == 16 && !it.isEmpty }
            if (john316 != null) {
                fontPreviewReference = ReferenceFormatter.format(BookCatalog.book(JOHN).displayName, 3, listOf(16))
                fontPreviewText = john316.text
            }
        }
    }

    fun setVerseLayout(value: VerseLayout) {
        viewModelScope.launch { settingsRepo.setVerseLayout(value) }
    }

    fun setAppLanguage(value: AppLanguage) {
        viewModelScope.launch { settingsRepo.setAppLanguage(value) }
    }

    /** Persists the choice, then swaps the launcher alias off the main thread. */
    fun setAppIcon(value: AppIcon) {
        viewModelScope.launch {
            settingsRepo.setAppIcon(value)
            withContext(Dispatchers.IO) { AppIconManager.apply(container.appContext, value) }
        }
    }

    fun setThemeMode(value: ThemeMode) {
        viewModelScope.launch { settingsRepo.setThemeMode(value) }
    }

    fun setAccent(value: AccentColor) {
        viewModelScope.launch { settingsRepo.setAccent(value) }
    }

    fun setFontSizeSp(value: Float) {
        viewModelScope.launch { settingsRepo.setFontSizeSp(value) }
    }

    fun setLineSpacing(value: LineSpacing) {
        viewModelScope.launch { settingsRepo.setLineSpacing(value) }
    }

    private companion object {
        const val PREVIEW_BOOK = 19 // Psalms
        const val PREVIEW_CHAPTER = 23
        val PREVIEW_VERSES = 1..2
        const val JOHN = 43
    }
}
