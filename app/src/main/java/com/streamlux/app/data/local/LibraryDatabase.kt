package com.streamlux.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE library_items ADD COLUMN isDownload INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE library_items ADD COLUMN downloadStatus TEXT")
        db.execSQL("ALTER TABLE library_items ADD COLUMN downloadProgress INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE library_items ADD COLUMN downloadQuality TEXT")
        db.execSQL("ALTER TABLE library_items ADD COLUMN downloadTotalBytes INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE library_items ADD COLUMN downloadedBytes INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(entities = [LibraryEntity::class], version = 4, exportSchema = false)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}

