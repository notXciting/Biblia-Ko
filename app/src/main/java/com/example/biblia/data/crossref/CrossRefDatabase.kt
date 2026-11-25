package com.example.biblia.data.crossref

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * One cross-reference: source verse key (CrossRefParser.key) -> target range,
 * book/chapter/verse only so it works in every Bible version.
 */
@Entity(tableName = "cross_refs", indices = [Index(value = ["sourceKey"])])
data class CrossRefEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceKey: Int,
    val toBook: Int,
    val toChapter: Int,
    val toVerse: Int,
    val toEndChapter: Int,
    val toEndVerse: Int,
    val votes: Int,
)

/** Import bookkeeping: present only after a complete import of [format]. */
@Entity(tableName = "cross_ref_meta")
data class CrossRefMetaEntity(@PrimaryKey val id: Int = 0, val format: Int)

@Dao
interface CrossRefDao {
    /** Best-voted first; [keys] covers a selection (e.g. both numbers of a combined verse). */
    @Query("SELECT * FROM cross_refs WHERE sourceKey IN (:keys) ORDER BY votes DESC, id LIMIT :limit")
    suspend fun forSources(keys: List<Int>, limit: Int): List<CrossRefEntity>

    @Insert
    fun insertAll(rows: List<CrossRefEntity>)

    @Query("DELETE FROM cross_refs")
    fun clear()

    @Query("SELECT format FROM cross_ref_meta WHERE id = 0")
    fun format(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setMeta(meta: CrossRefMetaEntity)
}

/**
 * Cross-references (OpenBible.info), re-importable from the bundled asset, so
 * it lives in its own file: never in user.db, and separate from the Bible
 * texts. A schema change may wipe it (the meta row goes too, so it re-imports).
 */
@Database(entities = [CrossRefEntity::class, CrossRefMetaEntity::class], version = 1, exportSchema = false)
abstract class CrossRefDatabase : RoomDatabase() {

    abstract fun crossRefDao(): CrossRefDao

    companion object {
        fun build(context: Context): CrossRefDatabase =
            Room.databaseBuilder(context.applicationContext, CrossRefDatabase::class.java, "crossrefs.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
