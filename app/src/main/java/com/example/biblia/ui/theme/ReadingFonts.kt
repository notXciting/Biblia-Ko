package com.example.biblia.ui.theme

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
import com.example.biblia.BuildConfig
import com.example.biblia.R
import com.example.biblia.domain.model.ReadingFont
import java.util.concurrent.ConcurrentHashMap

/**
 * The reading typeface in use: Bible text, verse numbers, chapter headings,
 * notes and search results. Buttons, menus and other UI keep the theme font.
 */
@Immutable
class ReadingType(val font: ReadingFont, val family: FontFamily, val lineHeight: Float)

private val SystemReading = ReadingType(ReadingFont.SYSTEM, FontFamily.Serif, 1f)

val LocalReadingType = staticCompositionLocalOf { SystemReading }

/** Resolves [font], falling back to the system serif when its files can't be loaded. */
@Composable
fun rememberReadingType(font: ReadingFont): ReadingType {
    val context = LocalContext.current.applicationContext
    return remember(font) {
        if (font == ReadingFont.SYSTEM || !ReadingFonts.usable(context, font)) SystemReading
        else ReadingType(font, ReadingFonts.family(font), font.lineHeight)
    }
}

object ReadingFonts {

    private class Face(val res: Int, val weight: FontWeight, val style: FontStyle)

    private fun faces(font: ReadingFont): List<Face> = when (font) {
        ReadingFont.MERRIWEATHER -> trio(R.font.merriweather_regular, R.font.merriweather_bold, R.font.merriweather_italic)
        ReadingFont.LORA -> trio(R.font.lora_regular, R.font.lora_bold, R.font.lora_italic)
        ReadingFont.EB_GARAMOND -> trio(R.font.ebgaramond_regular, R.font.ebgaramond_bold, R.font.ebgaramond_italic)
        ReadingFont.NOTO_SANS -> trio(R.font.notosans_regular, R.font.notosans_bold, R.font.notosans_italic)
        ReadingFont.ATKINSON -> trio(R.font.atkinson_regular, R.font.atkinson_bold, R.font.atkinson_italic)
        ReadingFont.OPEN_DYSLEXIC -> trio(R.font.opendyslexic_regular, R.font.opendyslexic_bold, R.font.opendyslexic_italic)
        ReadingFont.SYSTEM -> emptyList()
    }

    private fun trio(regular: Int, bold: Int, italic: Int) = listOf(
        Face(regular, FontWeight.Normal, FontStyle.Normal),
        Face(bold, FontWeight.Bold, FontStyle.Normal),
        Face(italic, FontWeight.Normal, FontStyle.Italic),
    )

    /** The family for [font]; the system serif for [ReadingFont.SYSTEM]. */
    fun family(font: ReadingFont): FontFamily =
        if (font == ReadingFont.SYSTEM) FontFamily.Serif
        else FontFamily(faces(font).map { Font(it.res, it.weight, it.style) })

    private val checked = ConcurrentHashMap<ReadingFont, Boolean>()

    /**
     * True when every face of [font] loads. Checked once per process: a broken
     * file would otherwise crash text layout. Uses the same platform cache as
     * Compose, so the reader doesn't parse the files twice.
     */
    fun usable(context: Context, font: ReadingFont): Boolean = checked.getOrPut(font) {
        faces(font).all { face ->
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.resources.getFont(face.res)
                else ResourcesCompat.getFont(context, face.res)
            }.onFailure {
                if (BuildConfig.DEBUG) Log.w("READING_FONT", "Can't load ${font.name}", it)
            }.getOrNull() != null
        }
    }
}
