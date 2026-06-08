package com.streamlux.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_items")
data class LibraryEntity(
    @PrimaryKey val id: String, // format: {mediaId} for movies/watchlist, {parentId}_s{N}_e{M} for episodes
    val mediaId: String,
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
    val systemDownloadId: Long? = null,   // Linked to Android DownloadManager ID
    val localUri: String? = null,         // Path to the downloaded file
    val parentId: String? = null,         // For TV shows (the series ID)
    val seriesTitle: String? = null,      // Name of the show (e.g. "Breaking Bad")
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeName: String? = null,      // Actual episode title (e.g. "Pilot")
    val episodeStillPath: String? = null, // TMDB still image path for the episode
    val timestamp: Long = System.currentTimeMillis()
)

