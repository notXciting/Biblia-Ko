package com.example.biblia.data.parser

import java.io.InputStream

/** Translation/copyright metadata from the root element of a Bible file. */
data class BibleMetadata(
    val translation: String,
    val copyright: String,
    val link: String,
)

/** Receives parse events. All callbacks are invoked sequentially on the calling thread. */
interface BibleParserListener {
    fun onMetadata(metadata: BibleMetadata)
    fun onBookStart(number: Int)
    fun onChapterStart(number: Int)
    fun onVerse(number: Int, text: String)
}

/**
 * Streaming parser contract. Implementations MUST NOT load the whole document
 * into memory — they push events to the listener as they read.
 */
interface BibleParser {
    fun parse(input: InputStream, listener: BibleParserListener)
}
