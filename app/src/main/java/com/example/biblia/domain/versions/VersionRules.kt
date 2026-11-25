package com.example.biblia.domain.versions

import com.example.biblia.data.BibleVersion
import kotlin.math.ceil

/** Pure rules for loading / unloading Bible versions (unit-tested). */
object VersionRules {

    /** Measured: ~83 MB of database for 10 Bibles of ~5.2 MB XML each. */
    private const val DB_PER_XML_BYTE = 1.6

    /**
     * A version can be unloaded when it is loaded, not the one being read, not
     * the last loaded one, and not importing right now.
     */
    fun canUnload(versionId: String, currentId: String, loaded: Set<String>, importing: Set<String>): Boolean =
        versionId in loaded && versionId != currentId && loaded.size > 1 && versionId !in importing

    /** The Manage list: Tagalog first, then the other languages, each in registry order. */
    fun sections(all: List<BibleVersion>): List<Pair<String, List<BibleVersion>>> =
        all.groupBy { it.language }.toList().sortedBy { (language, _) -> if (language == "Tagalog") 0 else 1 }

    /** Estimated space a loaded version takes ("~8 MB"), from its bundled XML size. */
    fun estimatedMb(assetBytes: Long): Int = ceil(assetBytes * DB_PER_XML_BYTE / 1_000_000.0).toInt().coerceAtLeast(1)
}
