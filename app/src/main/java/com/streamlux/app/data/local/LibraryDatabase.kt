package com.streamlux.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE library_items_new (
                id TEXT NOT NULL PRIMARY KEY,
                mediaId TEXT NOT NULL,
                mediaType TEXT NOT NULL,
                title TEXT NOT NULL,
                posterPath TEXT,
                isWatchlist INTEGER NOT NULL DEFAULT 0,
                isHistory INTEGER NOT NULL DEFAULT 0,
                isBookmarked INTEGER NOT NULL DEFAULT 0,
                isDownload INTEGER NOT NULL DEFAULT 0,
                downloadStatus TEXT,
                downloadProgress INTEGER NOT NULL DEFAULT 0,
                downloadQuality TEXT,
                downloadTotalBytes INTEGER NOT NULL DEFAULT 0,
                downloadedBytes INTEGER NOT NULL DEFAULT 0,
                systemDownloadId INTEGER,
                localUri TEXT,
                parentId TEXT,
                seriesTitle TEXT,
                seasonNumber INTEGER,
                episodeNumber INTEGER,
                timestamp INTEGER NOT NULL DEFAULT 0
            )
        """)
        db.execSQL("""
            INSERT INTO library_items_new (id, mediaId, mediaType, title, posterPath, isWatchlist, isHistory, isBookmarked, isDownload, downloadStatus, downloadProgress, downloadQuality, downloadTotalBytes, downloadedBytes, timestamp)
            SELECT mediaId, mediaId, mediaType, title, posterPath, isWatchlist, isHistory, isBookmarked, isDownload, downloadStatus, downloadProgress, downloadQuality, downloadTotalBytes, downloadedBytes, timestamp FROM library_items
        """)
        db.execSQL("DROP TABLE library_items")
        db.execSQL("ALTER TABLE library_items_new RENAME TO library_items")
    }
}

// Simple additive migration — just adds two nullable columns, all existing rows get NULL
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE library_items ADD COLUMN episodeName TEXT")
        db.execSQL("ALTER TABLE library_items ADD COLUMN episodeStillPath TEXT")
    }
}

@Database(entities = [LibraryEntity::class], version = 6, exportSchema = false)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
