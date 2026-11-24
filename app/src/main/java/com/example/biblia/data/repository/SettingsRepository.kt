package com.example.biblia.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.domain.model.AccentColor
import com.example.biblia.domain.model.AppIcon
import com.example.biblia.domain.model.AppLanguage
import com.example.biblia.domain.model.AppSettings
import com.example.biblia.domain.model.LineSpacing
import com.example.biblia.domain.model.NoteSort
import com.example.biblia.domain.model.SearchScope
import com.example.biblia.domain.model.ThemeMode
import com.example.biblia.domain.model.UiScale
import com.example.biblia.domain.model.VerseLayout
import com.example.biblia.domain.model.ViewMode
import com.example.biblia.domain.tts.TtsPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "bible_settings")

/** All user preferences, persisted with Preferences DataStore. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val APP_ICON = stringPreferencesKey("app_icon")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("accent_color")
        val FONT_SIZE = floatPreferencesKey("font_size_sp")
        val LINE_SPACING = stringPreferencesKey("line_spacing")
        val VERSE_LAYOUT = stringPreferencesKey("verse_layout")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SEARCH_SCOPE = stringPreferencesKey("search_scope")
        val VERSION_ID = stringPreferencesKey("version_id")
        val LAST_BOOK = intPreferencesKey("last_book")
        val LAST_CHAPTER = intPreferencesKey("last_chapter")
        val LAST_VERSE = intPreferencesKey("last_verse")
        val TTS_RATE = floatPreferencesKey("tts_rate")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val TTS_NUMBERS = booleanPreferencesKey("tts_speak_numbers")
        val SEARCH_INDEX_FORMAT = intPreferencesKey("search_index_format")
        val UI_SCALE = floatPreferencesKey("ui_scale")
        val NOTE_SORT = stringPreferencesKey("note_sort")
        fun ttsVoice(language: String) = stringPreferencesKey("tts_voice_" + language)
        fun imported(versionId: String) = booleanPreferencesKey("imported_" + versionId)
    }

    /** Reactive settings stream (safe to observe from the UI). */
    val settings: Flow<AppSettings> = context.settingsStore.data.map { p ->
        AppSettings(
            appLanguage = enumOrDefault(p[Keys.APP_LANGUAGE], AppLanguage.FOLLOW_VERSION),
            appIcon = enumOrDefault(p[Keys.APP_ICON], AppIcon.SUNRISE),
            themeMode = enumOrDefault(p[Keys.THEME_MODE], ThemeMode.SYSTEM),
            accent = enumOrDefault(p[Keys.ACCENT], AccentColor.BLUE),
            fontSizeSp = (p[Keys.FONT_SIZE] ?: 18f).coerceIn(12f, 32f),
            lineSpacing = enumOrDefault(p[Keys.LINE_SPACING], LineSpacing.NORMAL),
            verseLayout = enumOrDefault(p[Keys.VERSE_LAYOUT], VerseLayout.PARAGRAPH),
            viewMode = enumOrDefault(p[Keys.VIEW_MODE], ViewMode.LIST),
            searchScope = enumOrDefault(p[Keys.SEARCH_SCOPE], SearchScope.ALL),
            versionId = p[Keys.VERSION_ID] ?: BibleVersionRegistry.default.id,
            book = (p[Keys.LAST_BOOK] ?: 1).coerceIn(1, 66),
            chapter = (p[Keys.LAST_CHAPTER] ?: 1).coerceAtLeast(1),
            verse = (p[Keys.LAST_VERSE] ?: 0).coerceAtLeast(0),
            uiScale = UiScale.clamp(p[Keys.UI_SCALE] ?: UiScale.DEFAULT),
            noteSort = enumOrDefault(p[Keys.NOTE_SORT], NoteSort.RECENT),
            tts = TtsPrefs(
                rate = (p[Keys.TTS_RATE] ?: 1f).coerceIn(TtsPrefs.MIN_RATE, TtsPrefs.MAX_RATE),
                pitch = (p[Keys.TTS_PITCH] ?: 1f).coerceIn(TtsPrefs.MIN_PITCH, TtsPrefs.MAX_PITCH),
                voices = TTS_LANGUAGES.mapNotNull { lang -> p[Keys.ttsVoice(lang)]?.let { lang to it } }.toMap(),
                speakVerseNumbers = p[Keys.TTS_NUMBERS] ?: false,
            ),
        )
    }

    /** One-shot read used to initialize ViewModels without write/observe loops. */
    suspend fun initial(): AppSettings = settings.first()

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsStore.edit { block(it) }
    }

    suspend fun setAppLanguage(value: AppLanguage) = edit { it[Keys.APP_LANGUAGE] = value.name }
    suspend fun setAppIcon(value: AppIcon) = edit { it[Keys.APP_ICON] = value.name }
    suspend fun setThemeMode(value: ThemeMode) = edit { it[Keys.THEME_MODE] = value.name }
    suspend fun setAccent(value: AccentColor) = edit { it[Keys.ACCENT] = value.name }
    suspend fun setFontSizeSp(value: Float) = edit { it[Keys.FONT_SIZE] = value.coerceIn(12f, 32f) }
    suspend fun setLineSpacing(value: LineSpacing) = edit { it[Keys.LINE_SPACING] = value.name }
    suspend fun setVerseLayout(value: VerseLayout) = edit { it[Keys.VERSE_LAYOUT] = value.name }
    suspend fun setViewMode(value: ViewMode) = edit { it[Keys.VIEW_MODE] = value.name }
    suspend fun setSearchScope(value: SearchScope) = edit { it[Keys.SEARCH_SCOPE] = value.name }
    suspend fun setVersionId(value: String) = edit { it[Keys.VERSION_ID] = value }

    suspend fun setNoteSort(value: NoteSort) = edit { it[Keys.NOTE_SORT] = value.name }
    suspend fun setUiScale(value: Float) = edit { it[Keys.UI_SCALE] = UiScale.clamp(value) }
    suspend fun setTtsRate(value: Float) =
        edit { it[Keys.TTS_RATE] = value.coerceIn(TtsPrefs.MIN_RATE, TtsPrefs.MAX_RATE) }
    suspend fun setTtsPitch(value: Float) =
        edit { it[Keys.TTS_PITCH] = value.coerceIn(TtsPrefs.MIN_PITCH, TtsPrefs.MAX_PITCH) }
    suspend fun setTtsSpeakNumbers(value: Boolean) = edit { it[Keys.TTS_NUMBERS] = value }

    /** [voiceName] null = the engine's default voice for that language. */
    suspend fun setTtsVoice(language: String, voiceName: String?) = edit {
        if (voiceName == null) it.remove(Keys.ttsVoice(language)) else it[Keys.ttsVoice(language)] = voiceName
    }

    /**
     * Applies preferences from a backup in one write. Device state is kept:
     * the open Bible, reading position, installed versions and index format.
     */
    suspend fun restorePreferences(s: AppSettings) = edit { p ->
        p[Keys.APP_LANGUAGE] = s.appLanguage.name
        p[Keys.APP_ICON] = s.appIcon.name
        p[Keys.THEME_MODE] = s.themeMode.name
        p[Keys.ACCENT] = s.accent.name
        p[Keys.FONT_SIZE] = s.fontSizeSp.coerceIn(12f, 32f)
        p[Keys.LINE_SPACING] = s.lineSpacing.name
        p[Keys.VERSE_LAYOUT] = s.verseLayout.name
        p[Keys.VIEW_MODE] = s.viewMode.name
        p[Keys.SEARCH_SCOPE] = s.searchScope.name
        p[Keys.UI_SCALE] = UiScale.clamp(s.uiScale)
        p[Keys.NOTE_SORT] = s.noteSort.name
        p[Keys.TTS_RATE] = s.tts.rate.coerceIn(TtsPrefs.MIN_RATE, TtsPrefs.MAX_RATE)
        p[Keys.TTS_PITCH] = s.tts.pitch.coerceIn(TtsPrefs.MIN_PITCH, TtsPrefs.MAX_PITCH)
        p[Keys.TTS_NUMBERS] = s.tts.speakVerseNumbers
        // Voice names are per phone: unknown ones fall back to the best offline voice.
        s.tts.voices.forEach { (language, voice) -> p[Keys.ttsVoice(language)] = voice }
    }

    /** Persists the last read position (verse == 0 means "no focused verse"). */
    suspend fun setPosition(book: Int, chapter: Int, verse: Int) = edit {
        it[Keys.LAST_BOOK] = book.coerceIn(1, 66)
        it[Keys.LAST_CHAPTER] = chapter.coerceAtLeast(1)
        it[Keys.LAST_VERSE] = verse.coerceAtLeast(0)
    }

    /** Format of the FTS rows on disk (0 = before joined compounds were indexed). */
    suspend fun searchIndexFormat(): Int = context.settingsStore.data.first()[Keys.SEARCH_INDEX_FORMAT] ?: 0
    suspend fun setSearchIndexFormat(value: Int) = edit { it[Keys.SEARCH_INDEX_FORMAT] = value }

    suspend fun markImported(versionId: String) = edit { it[Keys.imported(versionId)] = true }

    fun isImportedFlow(versionId: String): Flow<Boolean> =
        context.settingsStore.data.map { it[Keys.imported(versionId)] ?: false }

    suspend fun isImported(versionId: String): Boolean = isImportedFlow(versionId).first()

    private companion object {
        val TTS_LANGUAGES = BibleVersionRegistry.all.map { it.language }.distinct()
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
