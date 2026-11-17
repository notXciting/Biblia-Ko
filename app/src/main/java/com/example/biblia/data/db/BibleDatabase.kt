package com.example.biblia.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VerseEntity::class, VerseFtsEntity::class, VersionMetaEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class BibleDatabase : RoomDatabase() {

    abstract fun bibleDao(): BibleDao

    companion object {
        fun build(context: Context): BibleDatabase =
            Room.databaseBuilder(context.applicationContext, BibleDatabase::class.java, "bible.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
