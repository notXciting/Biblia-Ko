package com.example.biblia.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One verse of one version. */
@Entity(
    tableName = "verses",
    indices = [
        Index(value = ["versionId", "book", "chapter", "verse"], unique = true),
    ],
)
data class VerseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val versionId: String,
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val text: String,
)

/** Per-version metadata (translation name, copyright, link) captured at import. */
@Entity(tableName = "version_meta")
data class VersionMetaEntity(
    @PrimaryKey val versionId: String,
    val translation: String,
    val copyright: String,
    val link: String,
    val importedAt: Long,
)

/** Projection row: highest chapter number per book for one version. */
data class BookChapterCountRow(
    val book: Int,
    val maxChapter: Int,
)

/** Projection row for rebuilding the search index. */
data class VerseTextRow(
    val id: Long,
    val text: String,
)
