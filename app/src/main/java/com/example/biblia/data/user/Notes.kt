package com.example.biblia.data.user

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * A personal note on one or more verses of a chapter. Version-independent
 * like highlights. [verses] is the sorted, comma-separated verse set
 * (see [com.example.biblia.domain.model.NoteVerses]); [firstVerse] orders notes.
 */
@Entity(tableName = "notes", indices = [Index(value = ["book", "chapter"])])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val book: Int,
    val chapter: Int,
    val firstVerse: Int,
    val verses: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE book = :book AND chapter = :chapter ORDER BY firstVerse, createdAt")
    fun observeChapter(book: Int, chapter: Int): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE book = :book AND chapter = :chapter")
    suspend fun chapter(book: Int, chapter: Int): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun byId(id: Long): NoteEntity?

    @Upsert
    suspend fun upsert(note: NoteEntity): Long

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}

/**
 * v1 -> v2: adds the notes table. Must match what Room generates for
 * [NoteEntity] exactly (checked against app/schemas/.../2.json).
 */
val USER_DB_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`book` INTEGER NOT NULL, `chapter` INTEGER NOT NULL, `firstVerse` INTEGER NOT NULL, " +
                "`verses` TEXT NOT NULL, `text` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_book_chapter` ON `notes` (`book`, `chapter`)")
    }
}
