package com.example.biblia.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery

/** Narrow projection for full-text search (only the columns the UI needs). */
data class VerseSearchRow(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val text: String,
)

/** In-memory chapter/verse count table row (loaded once at startup). */
data class ChapterVerseCountRow(
    val book: Int,
    val chapter: Int,
    val maxVerse: Int,
)

@Dao
interface BibleDao {

    // ---- Writes (used by the importer; called on IO threads) ----

    /** @return the generated row ids, aligned with the input list. */
    @Insert
    fun insertVerses(verses: List<VerseEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertVersionMeta(meta: VersionMetaEntity)

    /** Verse ids + text in canonical (id) order, for rebuilding the FTS index. */
    @Query("SELECT id, text FROM verses WHERE versionId = :versionId ORDER BY id")
    fun verseTexts(versionId: String): List<VerseTextRow>

    @Query("DELETE FROM version_meta WHERE versionId = :versionId")
    fun deleteVersionMeta(versionId: String)

    @Query("DELETE FROM verses WHERE versionId = :versionId")
    fun deleteVersesForVersion(versionId: String)

    // ---- Reads ----

    @Query(
        "SELECT * FROM verses WHERE versionId = :versionId AND book = :book AND chapter = :chapter ORDER BY verse"
    )
    suspend fun getChapter(versionId: String, book: Int, chapter: Int): List<VerseEntity>

    @Query(
        "SELECT MAX(verse) FROM verses WHERE versionId = :versionId AND book = :book AND chapter = :chapter"
    )
    suspend fun maxVerse(versionId: String, book: Int, chapter: Int): Int?

    @Query(
        "SELECT book, MAX(chapter) AS maxChapter FROM verses WHERE versionId = :versionId GROUP BY book ORDER BY book"
    )
    suspend fun chapterCounts(versionId: String): List<BookChapterCountRow>

    /** In-memory chapter/verse count table (loaded once at startup). */
    @Query(
        "SELECT book, chapter, MAX(verse) AS maxVerse FROM verses WHERE versionId = :versionId GROUP BY book, chapter"
    )
    suspend fun verseCounts(versionId: String): List<ChapterVerseCountRow>

    @Query("SELECT COUNT(*) FROM verses WHERE versionId = :versionId")
    suspend fun verseCount(versionId: String): Int

    @Query("SELECT * FROM version_meta WHERE versionId = :versionId")
    suspend fun versionMeta(versionId: String): VersionMetaEntity?

    /** Full-text search; SQL from [SearchSql.search] (per-version FTS tables Room doesn't know). */
    @RawQuery
    suspend fun search(query: SupportSQLiteQuery): List<VerseSearchRow>
}
