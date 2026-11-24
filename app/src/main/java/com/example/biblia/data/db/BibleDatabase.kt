package com.example.biblia.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Bible content (re-importable). v1 had one shared FTS4 table `verses_fts`
 * that also stored a second copy of every verse; v2 drops it in favour of
 * per-version contentless tables ([SearchSql], managed by [SearchIndex]),
 * which [com.example.biblia.data.importer.BibleImporter.upgradeSearchIndex]
 * rebuilds from the verses after the upgrade. Verses are kept.
 */
@Database(
    entities = [VerseEntity::class, VersionMetaEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class BibleDatabase : RoomDatabase() {

    abstract fun bibleDao(): BibleDao

    companion object {
        fun build(context: Context): BibleDatabase =
            Room.databaseBuilder(context.applicationContext, BibleDatabase::class.java, "bible.db")
                .addMigrations(BIBLE_DB_MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
    }
}

/** v1 -> v2: the shared FTS table goes (its shadow tables go with it). */
val BIBLE_DB_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS verses_fts")
    }
}
