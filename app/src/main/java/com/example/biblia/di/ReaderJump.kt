package com.example.biblia.di

import kotlinx.coroutines.withTimeoutOrNull

/**
 * Opens a verse in the reader from another screen (e.g. the notes list):
 * loads the chapter into the shared cache first (capped, like the selection
 * screen's jump) so the reader's first frame already shows the text, then
 * publishes the request. The caller pops back to the reader afterwards.
 */
suspend fun AppContainer.openInReader(versionId: String, book: Int, chapter: Int, verse: Int) {
    if (chapterCache.peek(versionId, book, chapter) == null) {
        withTimeoutOrNull(JUMP_LOAD_TIMEOUT_MS) { runCatching { chapterCache.get(versionId, book, chapter) } }
    }
    navigationBus.open(OpenVerseRequest(versionId, book, chapter, verse))
}

private const val JUMP_LOAD_TIMEOUT_MS = 400L
