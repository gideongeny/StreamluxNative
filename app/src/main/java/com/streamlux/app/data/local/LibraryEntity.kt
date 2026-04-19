package com.streamlux.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_items")
data class LibraryEntity(
    @PrimaryKey val mediaId: String,
    val mediaType: String,
    val title: String,
    val posterPath: String?,
    val isWatchlist: Boolean = false,
    val isHistory: Boolean = false,
    val isBookmarked: Boolean = false,
    val isDownload: Boolean = false,
    val downloadStatus: String? = null,   // "queued", "downloading", "paused", "completed", "failed"
    val downloadProgress: Int = 0,        // 0–100
    val downloadQuality: String? = null,  // "480p", "720p", "1080p"
    val downloadTotalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)
