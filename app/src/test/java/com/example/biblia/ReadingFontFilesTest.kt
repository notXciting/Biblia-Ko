package com.example.biblia

import com.example.biblia.domain.model.ReadingFont
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Every bundled reading font ships Regular/Bold/Italic plus its license (OFL requires it). */
class ReadingFontFilesTest {

    private val prefix = mapOf(
        ReadingFont.MERRIWEATHER to "merriweather",
        ReadingFont.LORA to "lora",
        ReadingFont.EB_GARAMOND to "ebgaramond",
        ReadingFont.NOTO_SANS to "notosans",
        ReadingFont.ATKINSON to "atkinson",
        ReadingFont.OPEN_DYSLEXIC to "opendyslexic",
    )

    // Unit tests run with the module directory (app/) as the working directory.
    private val main = File("src/main")

    @Test
    fun everyFontHasThreeFacesAndALicense() {
        assertEquals(ReadingFont.entries.toSet() - ReadingFont.SYSTEM, prefix.keys)
        for ((font, name) in prefix) {
            for (face in listOf("regular", "bold", "italic")) {
                val file = listOf("ttf", "otf").map { File(main, "res/font/${name}_$face.$it") }.firstOrNull { it.exists() }
                assertTrue("$font $face missing", file != null && file.length() > 10_000)
            }
            val license = File(main, "assets/licenses/${name}_ofl.txt")
            assertTrue("$font license missing", license.exists() && license.readText().contains("SIL OPEN FONT LICENSE"))
        }
    }

    @Test
    fun defaultsAndLineHeightsAreSane() {
        assertEquals(ReadingFont.MERRIWEATHER, ReadingFont.DEFAULT)
        assertEquals(1f, ReadingFont.SYSTEM.lineHeight)
        ReadingFont.entries.forEach { assertTrue(it.name, it.lineHeight in 0.9f..1.2f) }
    }
}
