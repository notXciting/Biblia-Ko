package com.example.biblia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.biblia.domain.model.AccentColor
import com.example.biblia.domain.model.AppSettings
import com.example.biblia.domain.model.ThemeMode

/** Surface colors of one reading theme (the accent is applied on top). */
class ThemePalette(
    val dark: Boolean,
    val background: Long,
    val onBackground: Long,
    val surfaceVariant: Long,
    val onSurfaceVariant: Long,
    val containerLowest: Long,
    val containerLow: Long,
    val container: Long,
    val containerHigh: Long,
    val containerHighest: Long,
    val outline: Long,
    val outlineVariant: Long,
    /** Paper only: the "desk" around the reader page (null = the page fills the screen). */
    val desk: Long? = null,
    /** Paper only: printed-Bible red for verse numbers (null = the accent colour). */
    val verseNumber: Long? = null,
)

/** Theme palettes, also used by the Settings theme previews. */
object ThemePalettes {
    val LIGHT = ThemePalette(
        false, 0xFFFBFAF8, 0xFF1D1B19, 0xFFF0EEEA, 0xFF615D57,
        0xFFFFFFFF, 0xFFF7F5F2, 0xFFF3F1ED, 0xFFEFECE8, 0xFFE9E6E1, 0xFFBDB8B1, 0xFFE3DFD9,
    )
    val SEPIA = ThemePalette(
        false, 0xFFF5ECDA, 0xFF433724, 0xFFEBDFC6, 0xFF6E5E42,
        0xFFFAF3E6, 0xFFF1E7D2, 0xFFEDE2CB, 0xFFE9DCC3, 0xFFE3D5BA, 0xFFB9A784, 0xFFE0D2B6,
    )
    /**
     * A printed Bible page: neutral warm-white paper (not Sepia's amber), near-black
     * blue-gray ink (not Sepia's brown), a light gray-beige desk around the page and
     * muted red verse numbers. Contrast is checked in ThemeContrastTest.
     */
    val PAPER = ThemePalette(
        false, 0xFFFBF9F4, 0xFF1E1E22, 0xFFF0EEE8, 0xFF55565C,
        0xFFFFFFFF, 0xFFF6F4EE, 0xFFF2F0EA, 0xFFEDEBE5, 0xFFE7E5DF, 0xFFB9B7B0, 0xFFE2DFD8,
        desk = 0xFFE6E3DC,
        verseNumber = 0xFFA3332B,
    )
    /** Soft green-gray paper: calm for long reading. */
    val SAGE = ThemePalette(
        false, 0xFFEEF2EC, 0xFF1E2620, 0xFFE3E9E0, 0xFF535E50,
        0xFFF7FAF5, 0xFFE9EEE6, 0xFFE5EBE2, 0xFFE0E6DC, 0xFFD9E0D5, 0xFFA7B1A2, 0xFFD6DDD2,
    )
    val DARK = ThemePalette(
        true, 0xFF141414, 0xFFE7E4E0, 0xFF232221, 0xFFA9A49D,
        0xFF0F0F0F, 0xFF1A1A19, 0xFF1E1E1D, 0xFF262625, 0xFF2F2E2D, 0xFF5E5A55, 0xFF34322F,
    )
    /** Dim blue-gray: softer than black in a dark room. */
    val SLATE = ThemePalette(
        true, 0xFF1A2029, 0xFFDDE3EA, 0xFF252D38, 0xFFA3AEBB,
        0xFF141A21, 0xFF1F2630, 0xFF232B35, 0xFF2A333E, 0xFF323C48, 0xFF5B6775, 0xFF333D49,
    )
    /** Pure black (OLED). */
    val MIDNIGHT = ThemePalette(
        true, 0xFF000000, 0xFFE2E2E2, 0xFF161616, 0xFFA0A0A0,
        0xFF000000, 0xFF0D0D0D, 0xFF121212, 0xFF1A1A1A, 0xFF222222, 0xFF4F4F4F, 0xFF262626,
    )

    fun of(mode: ThemeMode, systemDark: Boolean): ThemePalette = when (mode) {
        ThemeMode.SYSTEM -> if (systemDark) DARK else LIGHT
        ThemeMode.LIGHT -> LIGHT
        ThemeMode.SEPIA -> SEPIA
        ThemeMode.PAPER -> PAPER
        ThemeMode.SAGE -> SAGE
        ThemeMode.DARK -> DARK
        ThemeMode.SLATE -> SLATE
        ThemeMode.MIDNIGHT -> MIDNIGHT
    }
}

/** App theme built live from persisted settings. */
@Composable
fun BibleTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val palette = ThemePalettes.of(settings.themeMode, systemDark)
    val scheme = remember(palette, settings.accent) { buildScheme(palette, settings.accent) }
    val page = remember(palette, settings.paperGrain) {
        palette.desk?.let { desk ->
            PageStyle(Color(desk), Color(palette.verseNumber ?: palette.onBackground), settings.paperGrain)
        }
    }
    CompositionLocalProvider(
        LocalPageStyle provides page,
        LocalReadingType provides rememberReadingType(settings.readingFont),
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/**
 * Color scheme = theme surfaces + the accent tone tuned for that brightness.
 * Tinted containers are opaque blends of accent + background.
 */
fun buildScheme(p: ThemePalette, accent: AccentColor): ColorScheme {
    val bg = Color(p.background)
    val primary = Color(accent.tone(p.dark))
    val container = lerp(bg, primary, if (p.dark) 0.24f else 0.14f)
    val onContainer = if (p.dark) lerp(primary, Color.White, 0.45f) else lerp(primary, Color.Black, 0.35f)
    val onPrimary = if (p.dark) Color(0xFF101010) else Color.White
    val base = if (p.dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        secondary = primary,
        onSecondary = onPrimary,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        secondaryContainer = container,
        onSecondaryContainer = onContainer,
        background = bg,
        onBackground = Color(p.onBackground),
        surface = bg,
        onSurface = Color(p.onBackground),
        surfaceVariant = Color(p.surfaceVariant),
        onSurfaceVariant = Color(p.onSurfaceVariant),
        surfaceContainerLowest = Color(p.containerLowest),
        surfaceContainerLow = Color(p.containerLow),
        surfaceContainer = Color(p.container),
        surfaceContainerHigh = Color(p.containerHigh),
        surfaceContainerHighest = Color(p.containerHighest),
        outline = Color(p.outline),
        outlineVariant = Color(p.outlineVariant),
    )
}

/**
 * Printed-page look of the reader, provided only by themes with a desk colour
 * (Paper). Null for every other theme, so they render exactly as before.
 */
class PageStyle(val desk: Color, val verseNumber: Color, val grain: Boolean)

val LocalPageStyle = staticCompositionLocalOf<PageStyle?> { null }
