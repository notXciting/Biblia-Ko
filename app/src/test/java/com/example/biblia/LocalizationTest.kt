package com.example.biblia

import com.example.biblia.data.BibleVersionRegistry
import com.example.biblia.data.catalog.BookCatalog
import com.example.biblia.domain.model.AppLanguage
import com.example.biblia.domain.model.AppSettings
import com.example.biblia.domain.model.BookNameLanguage
import com.example.biblia.ui.uiLocaleTag
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LocalizationTest {

    @After fun reset() { BookNameLanguage.english = false }

    private fun res(path: String): File =
        listOf(File("src/main/res/$path"), File("app/src/main/res/$path")).first { it.exists() }

    private fun keys(path: String): Set<String> =
        Regex("""<string name="([^"]+)"""").findAll(res(path).readText()).map { it.groupValues[1] }.toSet()

    @Test
    fun everyTagalogStringHasAnEnglishTranslation() {
        val missing = keys("values/strings.xml") - keys("values-en/strings.xml") - setOf("app_name")
        assertTrue("Missing in values-en: $missing", missing.isEmpty())
    }

    @Test
    fun bookNamesFollowLanguageFlag() {
        val numbers = BookCatalog.book(4)
        BookNameLanguage.english = false
        assertEquals("Mga Bilang", numbers.displayName)
        assertEquals("Numbers", numbers.alternateName)
        BookNameLanguage.english = true
        assertEquals("Numbers", numbers.displayName)
        assertEquals("Mga Bilang", numbers.alternateName)
    }

    @Test
    fun uiLocaleFollowsVersionOrFixedChoice() {
        assertEquals("fil", uiLocaleTag(AppSettings(appLanguage = AppLanguage.FOLLOW_VERSION, versionId = "mbb12")))
        assertEquals("en", uiLocaleTag(AppSettings(appLanguage = AppLanguage.FOLLOW_VERSION, versionId = "esv")))
        assertEquals("en", uiLocaleTag(AppSettings(appLanguage = AppLanguage.ENGLISH, versionId = "mbb12")))
        assertEquals("fil", uiLocaleTag(AppSettings(appLanguage = AppLanguage.TAGALOG, versionId = "niv")))
        assertTrue(BibleVersionRegistry.byId("amp").isEnglish)
    }
}
