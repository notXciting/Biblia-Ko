package com.example.biblia.domain.tts

import com.example.biblia.data.catalog.BookCatalog
import java.util.Locale

/** Maps a Bible version's language to the TTS locale(s) to try, in order. */
object TtsLocales {

    /** Tagalog engines register as "fil-PH" (Google) or the older "tl-PH". */
    fun candidates(language: String): List<Locale> = when (language) {
        "Tagalog" -> listOf(Locale("fil", "PH"), Locale("tl", "PH"))
        else -> listOf(Locale.US)
    }

    /** ISO language codes a voice may carry for [language]. */
    fun languageCodes(language: String): Set<String> = when (language) {
        "Tagalog" -> setOf("fil", "tl")
        else -> setOf("en")
    }
}

/** Canonical chapter stepping across book boundaries (Genesis 1 .. Revelation 22). */
object ChapterSteps {

    fun next(book: Int, chapter: Int): Pair<Int, Int>? = when {
        chapter < BookCatalog.standardChapterCount(book) -> book to chapter + 1
        book < 66 -> book + 1 to 1
        else -> null
    }

    fun previous(book: Int, chapter: Int): Pair<Int, Int>? = when {
        chapter > 1 -> book to chapter - 1
        book > 1 -> (book - 1).let { it to BookCatalog.standardChapterCount(it) }
        else -> null
    }
}
