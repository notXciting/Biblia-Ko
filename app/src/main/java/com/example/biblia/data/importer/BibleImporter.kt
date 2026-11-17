package com.example.biblia.data.importer

import android.content.Context
import android.util.Log
import com.example.biblia.data.BibleVersion
import com.example.biblia.data.db.BibleDatabase
import com.example.biblia.data.db.VerseEntity
import com.example.biblia.data.db.VerseFtsEntity
import com.example.biblia.data.db.VersionMetaEntity
import com.example.biblia.data.parser.BibleMetadata
import com.example.biblia.data.parser.BibleParser
import com.example.biblia.data.parser.BibleParserListener
import com.example.biblia.data.repository.SettingsRepository
import com.example.biblia.domain.search.TextNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Imports a Bible version from assets into Room. Owned by the Application
 * scope: import survives rotation and runs once per version (DataStore flag).
 * Streaming: at most [BATCH_SIZE] verses in memory, inserted in batched
 * transactions, with byte-accurate progress published via [states].
 */
class BibleImporter(
    private val context: Context,
    private val database: BibleDatabase,
    private val settings: SettingsRepository,
    private val parserSelector: (BibleVersion) -> BibleParser,
    private val onImported: (versionId: String) -> Unit = {},
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val _states = MutableStateFlow<Map<String, ImportState>>(emptyMap())
    val states: StateFlow<Map<String, ImportState>> = _states.asStateFlow()

    fun stateFor(versionId: String): ImportState =
        _states.value[versionId] ?: ImportState(versionId)

    /** Fire-and-forget import, used from the UI layer. */
    fun ensureImportedAsync(version: BibleVersion) {
        scope.launch { ensureImported(version) }
    }

    /** @return true when the version is available (already imported or just imported). */
    suspend fun ensureImported(version: BibleVersion): Boolean {
        if (settings.isImported(version.id)) {
            setState(version.id, ImportState.Phase.DONE, 1f)
            return true
        }
        return mutex.withLock {
            if (settings.isImported(version.id)) {
                setState(version.id, ImportState.Phase.DONE, 1f)
                true
            } else {
                try {
                    doImport(version)
                    true
                } catch (t: Throwable) {
                    Log.e(TAG, "Import failed for " + version.id, t)
                    setState(version.id, ImportState.Phase.FAILED, 0f)
                    false
                }
            }
        }
    }

    private suspend fun doImport(version: BibleVersion) {
        val dao = database.bibleDao()
        setState(version.id, ImportState.Phase.RUNNING, 0f)

        // Clear any partial data from a previously interrupted import.
        dao.deleteFtsForVersion(version.id)
        dao.deleteVersesForVersion(version.id)

        val cacheFile = context.copyBibleAssetToCache(version)
        val totalBytes = cacheFile.length().coerceAtLeast(1L)
        var lastPercent = -1
        val pendingVerses = ArrayList<VerseEntity>(BATCH_SIZE)

        fun flush() {
            if (pendingVerses.isEmpty()) return
            database.runInTransaction {
                val ids = dao.insertVerses(pendingVerses)
                val ftsRows = ArrayList<VerseFtsEntity>(ids.size)
                for (i in ids.indices) {
                    val verse = pendingVerses[i]
                    ftsRows.add(VerseFtsEntity(verseId = ids[i], normText = TextNormalizer.fold(verse.text.lowercase())))
                }
                dao.insertVerseFts(ftsRows)
            }
            pendingVerses.clear()
        }

        val listener = object : BibleParserListener {
            var book = 0
            var chapter = 0
            override fun onMetadata(metadata: BibleMetadata) {
                // Registry values win: XML headers are inconsistent between files.
                dao.insertVersionMeta(
                    VersionMetaEntity(
                        versionId = version.id,
                        translation = version.fullName.ifBlank { metadata.translation },
                        copyright = version.copyright.ifBlank { metadata.copyright },
                        link = metadata.link,
                        importedAt = System.currentTimeMillis(),
                    )
                )
            }
            override fun onBookStart(number: Int) { book = number }
            override fun onChapterStart(number: Int) { chapter = number }
            override fun onVerse(number: Int, text: String) {
                pendingVerses.add(
                    VerseEntity(versionId = version.id, book = book, chapter = chapter, verse = number, text = text)
                )
                if (pendingVerses.size >= BATCH_SIZE) flush()
            }
        }

        val input = ProgressInputStream(cacheFile.inputStream(), totalBytes) { fraction ->
            val percent = (fraction * 100).toInt().coerceIn(0, 100)
            if (percent != lastPercent) {
                lastPercent = percent
                setState(version.id, ImportState.Phase.RUNNING, fraction.coerceIn(0f, 1f))
            }
        }
        input.use { stream ->
            parserSelector(version).parse(BomStreams.stripBom(stream), listener)
        }
        flush()

        if (dao.verseCount(version.id) == 0) error("Import produced 0 verses for " + version.id)

        settings.markImported(version.id)
        onImported(version.id)
        setState(version.id, ImportState.Phase.DONE, 1f)
    }

    private fun setState(versionId: String, phase: ImportState.Phase, progress: Float) {
        _states.update { current -> current + (versionId to ImportState(versionId, phase, progress)) }
    }

    companion object {
        private const val TAG = "BibleImporter"
        private const val BATCH_SIZE = 400
    }
}
