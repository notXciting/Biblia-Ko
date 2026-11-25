package com.example.biblia

import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.data.db.SearchSql
import com.example.biblia.domain.versions.VersionRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionRulesTest {

    private val loaded = setOf("mbb12", "kjv", "niv")

    @Test
    fun onlyLoadedNonCurrentIdleVersionsCanBeUnloaded() {
        assertTrue(VersionRules.canUnload("kjv", currentId = "mbb12", loaded = loaded, importing = emptySet()))
        assertFalse("the Bible being read", VersionRules.canUnload("mbb12", "mbb12", loaded, emptySet()))
        assertFalse("not loaded", VersionRules.canUnload("esv", "mbb12", loaded, emptySet()))
        assertFalse("importing", VersionRules.canUnload("kjv", "mbb12", loaded, setOf("kjv")))
        assertFalse("last loaded", VersionRules.canUnload("kjv", "mbb12", setOf("kjv"), emptySet()))
    }

    @Test
    fun manageListGroupsTagalogFirstInRegistryOrder() {
        val sections = VersionRules.sections(BibleVersionRegistry.all)
        assertEquals(listOf("Tagalog", "English"), sections.map { it.first })
        assertEquals(listOf("mbb12", "asd"), sections[0].second.map { it.id })
        assertEquals(
            listOf("amp", "csb", "esv", "hcsb", "kjv", "nasb", "niv", "nkjv", "nlt"),
            sections[1].second.map { it.id },
        )
    }

    @Test
    fun sizeEstimateFromTheBundledXml() {
        assertEquals(9, VersionRules.estimatedMb(5_085_269)) // NASB: ~8.1 MB -> "~9 MB" (rounded up)
        assertEquals(1, VersionRules.estimatedMb(0))
    }

    @Test
    fun nasbIsRegisteredAndEveryIdIsUniqueAndSafe() {
        val nasb = BibleVersionRegistry.byId("nasb")
        assertEquals("NASB", nasb.shortName)
        assertEquals("bibles/EnglishNASBBible.xml", nasb.assetPath)
        assertEquals(11, BibleVersionRegistry.all.size)
        assertEquals(BibleVersionRegistry.all.size, BibleVersionRegistry.all.map { it.id }.toSet().size)
        BibleVersionRegistry.all.forEach { SearchSql.table(it.id) } // throws on an unsafe id
    }
}
