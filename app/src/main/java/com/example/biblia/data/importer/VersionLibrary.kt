package com.example.biblia.data.importer

import android.content.Context
import com.example.biblia.data.BibleVersion
import com.example.biblia.data.db.BibleDatabase
import com.example.biblia.data.db.SearchIndex
import com.example.biblia.data.repository.BibleRepository
import com.example.biblia.data.repository.ChapterCache
import com.example.biblia.data.repository.SettingsRepository
import com.example.biblia.domain.versions.VersionRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Which Bibles are on the phone ("Pamahalaan ang mga Biblia"): loading goes
 * through [BibleImporter]; unloading deletes a version's verses, search index
 * and metadata and gives the space back. Highlights and notes live in user.db
 * and don't depend on a version, so they are never touched.
 */
class VersionLibrary(
    private val context: Context,
    private val database: BibleDatabase,
    private val settings: SettingsRepository,
    private val searchIndex: SearchIndex,
    private val importer: BibleImporter,
    private val chapterCache: ChapterCache,
    private val bibleRepository: BibleRepository,
) {

    val loaded: Flow<Set<String>> = settings.loadedVersionIds

    private val sizes = ConcurrentHashMap<String, Int>()

    fun load(version: BibleVersion) = importer.ensureImportedAsync(version)

    /** "~8 MB": estimated from the bundled XML (asset length read once, then cached). */
    suspend fun estimatedMb(version: BibleVersion): Int = sizes[version.id] ?: withContext(Dispatchers.IO) {
        val bytes = runCatching { context.assets.openFd(version.assetPath).use { it.length } }.getOrNull()
            ?: runCatching { context.assets.open(version.assetPath).use(::countBytes) }.getOrDefault(0L)
        VersionRules.estimatedMb(bytes).also { sizes[version.id] = it }
    }

    /** @return false when [VersionRules.canUnload] forbids it (current, last loaded, importing). */
    suspend fun unload(version: BibleVersion): Boolean = withContext(Dispatchers.IO) {
        val removed = importer.exclusive {
            val current = settings.initial().versionId
            val importing = importer.states.value.filterValues { it.isRunning }.keys
            if (!VersionRules.canUnload(version.id, current, settings.loadedVersionIds.first(), importing)) return@exclusive false
            searchIndex.drop(version.id)
            val dao = database.bibleDao()
            database.runInTransaction {
                dao.deleteVersesForVersion(version.id)
                dao.deleteVersionMeta(version.id)
            }
            settings.unmarkImported(version.id)
            importer.forget(version.id)
            bibleRepository.invalidateCounts(version.id)
            chapterCache.invalidate(version.id)
            true
        }
        // Give the ~8 MB back to the phone (outside the lock: VACUUM takes a few seconds).
        if (removed) searchIndex.compact(vacuum = true)
        removed
    }
}

/** Length of a compressed asset (openFd only works for uncompressed ones). */
private fun countBytes(input: java.io.InputStream): Long {
    val buffer = ByteArray(64 * 1024)
    var total = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) return total
        total += read
    }
}
