package com.example.biblia

import com.example.biblia.domain.model.AccentColor
import com.example.biblia.domain.model.HighlightColor
import com.example.biblia.domain.model.ThemeMode
import com.example.biblia.ui.theme.ThemePalettes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * WCAG contrast guarantees for every theme x accent x highlight combination.
 * Highlight alphas mirror ChapterPage (0.55 light themes, 0.30 dark themes).
 */
class ThemeContrastTest {

    private fun channel(c: Long, shift: Int) = ((c shr shift) and 0xFF) / 255.0
    private fun lin(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    private fun lum(c: Long) =
        0.2126 * lin(channel(c, 16)) + 0.7152 * lin(channel(c, 8)) + 0.0722 * lin(channel(c, 0))
    private fun contrast(a: Long, b: Long): Double {
        val (hi, lo) = listOf(lum(a), lum(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }
    private fun over(fg: Long, alpha: Double, bg: Long): Long {
        fun mix(s: Int) = Math.round(((bg shr s) and 0xFF) * (1 - alpha) + ((fg shr s) and 0xFF) * alpha)
        return (0xFFL shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    private val themes = ThemeMode.entries.filter { it != ThemeMode.SYSTEM }
        .map { ThemePalettes.of(it, systemDark = false) }

    @Test
    fun systemIsTheFirstThemeOption() = assertEquals(ThemeMode.SYSTEM, ThemeMode.entries.first())

    @Test
    fun accentTextAndButtonsReadableOnEveryTheme() {
        for (p in themes) for (a in AccentColor.entries) {
            val tone = a.tone(p.dark)
            val label = if (p.dark) 0xFF101010 else 0xFFFFFFFF
            assertTrue("$a text on ${p.background}", contrast(tone, p.background) >= 4.5)
            assertTrue("$a button on ${p.background}", contrast(label, tone) >= 4.5)
        }
    }

    @Test
    fun highlightsKeepVerseTextAndNumbersReadable() {
        for (p in themes) {
            val alpha = if (p.dark) 0.30 else 0.55
            for (h in HighlightColor.entries) {
                val bg = over(h.argb, alpha, p.background)
                assertTrue("text on $h", contrast(p.onBackground, bg) >= 4.5)
                for (a in AccentColor.entries) {
                    assertTrue("$a number on $h", contrast(a.tone(p.dark), bg) >= 3.0)
                }
            }
        }
    }

    @Test
    fun secondaryTextReadable() {
        for (p in themes) {
            assertTrue(contrast(p.onSurfaceVariant, p.background) >= 4.5)
            assertTrue(contrast(p.onSurfaceVariant, p.containerHigh) >= 4.5)
        }
    }

    @Test
    fun paperInkAndRedVerseNumbersReadableOnPageAndHighlights() {
        val paper = ThemePalettes.PAPER
        val red = paper.verseNumber!!
        assertTrue("ink", contrast(paper.onBackground, paper.background) >= 4.5)
        assertTrue("red numbers", contrast(red, paper.background) >= 4.5)
        for (h in HighlightColor.entries) {
            val bg = over(h.argb, 0.55, paper.background)
            assertTrue("ink on $h", contrast(paper.onBackground, bg) >= 4.5)
            assertTrue("red number on $h", contrast(red, bg) >= 3.0)
        }
        // Clearly not Sepia: lighter paper, and ink is not brown.
        assertTrue(lum(paper.background) > lum(ThemePalettes.SEPIA.background) + 0.08)
        assertTrue(paper.desk != null && ThemePalettes.SEPIA.desk == null)
    }
}
