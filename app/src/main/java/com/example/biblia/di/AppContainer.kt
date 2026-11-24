package com.example.biblia.di

import android.content.Context
import com.example.biblia.data.BibleVersion
import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.data.db.BibleDatabase
import com.example.biblia.data.db.SearchIndex
import com.example.biblia.data.importer.BibleImporter
import com.example.biblia.data.parser.BibleParser
import com.example.biblia.data.parser.TagalogXmlBibleParser
import com.example.biblia.data.repository.BibleRepository
import com.example.biblia.data.repository.ChapterCache
import com.example.biblia.data.repository.SettingsRepository
import com.example.biblia.data.user.HighlightRepository
import com.example.biblia.data.user.NoteRepository
import com.example.biblia.data.user.UserDatabase
import com.example.biblia.data.tts.AndroidPlaybackFocus
import com.example.biblia.data.tts.AndroidTtsEngine
import com.example.biblia.domain.tts.TtsController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Manual dependency injection container, owned by [com.example.biblia.BibleApplication].
 * [navigationBus] is THE single navigation bus shared by the reader and selection
 * ViewModels; [chapterCache] is the shared LRU used for pre-jump preload.
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val database: BibleDatabase = BibleDatabase.build(appContext)
    val settingsRepository = SettingsRepository(appContext)
    /** Per-version full-text search tables (outside Room's schema). */
    val searchIndex = SearchIndex(database)
    val bibleRepository = BibleRepository(database.bibleDao(), searchIndex)
    val navigationBus = NavigationBus()

    /** Application-wide background scope for fire-and-forget preloads. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val chapterCache = ChapterCache(bibleRepository)

    /** User data (highlights): separate DB that is never destructively migrated. */
    val userDatabase: UserDatabase = UserDatabase.build(appContext)
    val highlightRepository = HighlightRepository(userDatabase.highlightDao())
    val noteRepository = NoteRepository(userDatabase.noteDao())

    private val parsers: Map<String, BibleParser> = mapOf(
        BibleVersion.FORMAT_TAGALOG_XML to TagalogXmlBibleParser(),
    )

    val importer = BibleImporter(
        context = appContext,
        database = database,
        settings = settingsRepository,
        searchIndex = searchIndex,
        parserSelector = { version: BibleVersion ->
            parsers[version.formatId] ?: TagalogXmlBibleParser()
        },
        onImported = { versionId: String -> bibleRepository.invalidateCounts(versionId) },
    )

    /** Main-thread scope for "Listen" (TTS state and callbacks live here). */
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** "Listen": the TextToSpeech engine is created lazily on first use. */
    val tts = TtsController(
        engineFactory = { listener -> AndroidTtsEngine(appContext, listener) },
        focus = AndroidPlaybackFocus(appContext),
        source = { versionId, book, chapter -> chapterCache.get(versionId, book, chapter).verses },
        scope = mainScope,
    )

    init {
        // Re-index installed Bibles once when the search index format changes.
        applicationScope.launch {
            runCatching {
                importer.upgradeSearchIndex(BibleVersionRegistry.all, settingsRepository.initial().versionId)
            }
        }
        mainScope.launch {
            settingsRepository.settings.map { it.tts }.distinctUntilChanged().collect { tts.updatePrefs(it) }
        }
    }
}
