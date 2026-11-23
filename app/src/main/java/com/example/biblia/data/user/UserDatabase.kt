package com.example.biblia.data.user

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * A verse highlight. Keyed by book/chapter/verse only (not by version), so a
 * highlight made in one translation shows in every translation.
 */
@Entity(tableName = "highlights", primaryKeys = ["book", "chapter", "verse"])
data class HighlightEntity(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val color: String,
    val updatedAt: Long,
)

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights WHERE book = :book AND chapter = :chapter")
    fun observeChapter(book: Int, chapter: Int): Flow<List<HighlightEntity>>

    @Upsert
    suspend fun upsert(rows: List<HighlightEntity>)

    @Query("DELETE FROM highlights WHERE book = :book AND chapter = :chapter AND verse IN (:verses)")
    suspend fun delete(book: Int, chapter: Int, verses: List<Int>)
}

/**
 * The user's OWN data (highlights, notes). Deliberately separate from the
 * Bible content database, which may be wiped and re-imported: this one must
 * never use fallbackToDestructiveMigration — any schema change needs a
 * Migration. Schemas are exported to app/schemas to check migrations against.
 *
 * v1: highlights. v2: + notes ([USER_DB_MIGRATION_1_2]).
 */
@Database(entities = [HighlightEntity::class, NoteEntity::class], version = 2, exportSchema = true)
abstract class UserDatabase : RoomDatabase() {

    abstract fun highlightDao(): HighlightDao
    abstract fun noteDao(): NoteDao

    companion object {
        fun build(context: Context): UserDatabase =
            Room.databaseBuilder(context.applicationContext, UserDatabase::class.java, "user.db")
                .addMigrations(USER_DB_MIGRATION_1_2)
                .build()
    }
}
