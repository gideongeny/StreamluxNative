package com.streamlux.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.streamlux.app.data.local.LibraryDao
import com.streamlux.app.data.local.LibraryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.app.DownloadManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val dao: LibraryDao,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _watchlist = MutableStateFlow<List<LibraryEntity>>(emptyList())
    val watchlist: StateFlow<List<LibraryEntity>> = _watchlist

    private val _history = MutableStateFlow<List<LibraryEntity>>(emptyList())
    val history: StateFlow<List<LibraryEntity>> = _history

    private val _bookmarked = MutableStateFlow<List<LibraryEntity>>(emptyList())
    val bookmarked: StateFlow<List<LibraryEntity>> = _bookmarked

    private val _downloads = MutableStateFlow<List<LibraryEntity>>(emptyList())
    val downloads: StateFlow<List<LibraryEntity>> = _downloads

    init {
        fetchFromLocal()
        fetchFromFirestore()
        startProgressPolling()
    }

    private fun startProgressPolling() {
        viewModelScope.launch {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            while (true) {
                val activeDownloads = _downloads.value.filter { 
                    it.downloadStatus == "downloading" || it.downloadStatus == "queued" 
                }
                
                if (activeDownloads.isNotEmpty()) {
                    activeDownloads.forEach { item ->
                        val downloadId = item.systemDownloadId ?: return@forEach
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val cursor = dm.query(query)
                        if (cursor.moveToFirst()) {
                            val bytesSoFar = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            val totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                            
                            val progress = if (totalBytes > 0) ((bytesSoFar * 100) / totalBytes).toInt() else 0
                            
                            // Map DM status to our status
                            val newStatus = when(status) {
                                DownloadManager.STATUS_RUNNING -> "downloading"
                                DownloadManager.STATUS_PENDING -> "queued"
                                DownloadManager.STATUS_PAUSED -> "paused"
                                DownloadManager.STATUS_SUCCESSFUL -> "completed"
                                DownloadManager.STATUS_FAILED -> "failed"
                                else -> item.downloadStatus
                            }

                            if (progress != item.downloadProgress || newStatus != item.downloadStatus) {
                                dao.insertItem(item.copy(
                                    downloadProgress = progress,
                                    downloadStatus = newStatus,
                                    downloadedBytes = bytesSoFar,
                                    downloadTotalBytes = totalBytes
                                ))
                            }
                        }
                        cursor.close()
                    }
                }
                delay(1500) // Poll every 1.5 seconds
            }
        }
    }

    private fun fetchFromLocal() {
        viewModelScope.launch {
            dao.getHistory().collectLatest { localHistory ->
                _history.value = localHistory
            }
        }
        viewModelScope.launch {
            dao.getWatchlist().collectLatest { localWatchlist ->
                _watchlist.value = localWatchlist
            }
        }
        viewModelScope.launch {
            dao.getDownloads().collectLatest { localDownloads ->
                // Local DB is the primary source for downloads; Firestore listener
                // will upsert / update entries when cloud changes arrive.
                _downloads.value = localDownloads
            }
        }
    }

    private fun fetchFromFirestore() {
        val user = auth.currentUser ?: return

        // 1. Cloud History Sync (Merging with local)
        firestore.collection("continueWatching")
            .document(user.uid)
            .collection("items")
            .orderBy("lastWatched", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("LibraryVM", "Firestore history error: ${e.message}")
                    return@addSnapshotListener
                }
                val remoteItems = snapshot?.documents?.mapNotNull { doc ->
                    LibraryEntity(
                        id = doc.id,
                        mediaId = doc.id,
                        mediaType = doc.getString("type") ?: "movie",
                        title = doc.getString("title") ?: "Unknown",
                        posterPath = doc.getString("thumbnail")?.replace("https://image.tmdb.org/t/p/w500", ""),
                        isHistory = true,
                        timestamp = doc.getLong("lastWatched") ?: 0
                    )
                } ?: emptyList()

                val current = _history.value.toMutableList()
                remoteItems.forEach { remote ->
                    if (current.none { it.mediaId == remote.mediaId }) {
                        current.add(remote)
                    }
                }
                _history.value = current.sortedByDescending { it.timestamp }
            }

        // 2. Cloud Watchlist Sync
        firestore.collection("users")
            .document(user.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val bookmarks = snapshot?.get("bookmarks") as? List<Map<String, Any>> ?: emptyList()
                val remoteWatchlist = bookmarks.map { map ->
                    val mid = map["id"]?.toString() ?: ""
                    LibraryEntity(
                        id = mid,
                        mediaId = mid,
                        mediaType = map["media_type"]?.toString() ?: "movie",
                        title = map["title"]?.toString() ?: "Unknown",
                        posterPath = map["poster_path"]?.toString(),
                        isWatchlist = true,
                        isBookmarked = true
                    )
                }

                val current = _watchlist.value.toMutableList()
                remoteWatchlist.forEach { remote ->
                    if (current.none { it.mediaId == remote.mediaId }) {
                        current.add(remote)
                    }
                }
                _watchlist.value = current
                _bookmarked.value = current
            }

        // 3. Cloud Downloads Sync — real-time listener on users/{uid}/downloads
        firestore.collection("users")
            .document(user.uid)
            .collection("downloads")
            .orderBy("addedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("LibraryVM", "Firestore downloads error: ${e.message}")
                    return@addSnapshotListener
                }

                val remoteDownloads = snapshot?.documents?.mapNotNull { doc ->
                    LibraryEntity(
                        id = doc.id,
                        mediaId = doc.getString("mediaId") ?: "",
                        mediaType = doc.getString("mediaType") ?: "movie",
                        title = doc.getString("title") ?: "Unknown",
                        posterPath = doc.getString("thumbnail"),
                        isDownload = true,
                        downloadStatus = doc.getString("status"),
                        downloadProgress = (doc.getLong("progress") ?: 0L).toInt(),
                        downloadQuality = doc.getString("quality"),
                        downloadTotalBytes = doc.getLong("totalBytes") ?: 0L,
                        downloadedBytes = doc.getLong("downloadedBytes") ?: 0L,
                        systemDownloadId = doc.getLong("systemId"),
                        parentId = doc.getString("parentId"),
                        seriesTitle = doc.getString("seriesTitle"),
                        seasonNumber = doc.getLong("season")?.toInt(),
                        episodeNumber = doc.getLong("episode")?.toInt(),
                        timestamp = doc.getLong("addedAt") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()

                // Upsert each remote download into local Room DB for offline access
                // The UI will reactively update via the Room Flow in fetchFromLocal()
                viewModelScope.launch {
                    remoteDownloads.forEach { dao.insertItem(it) }
                }
            }
    }

    /** Remove a download entry (called from UI on swipe-to-delete). */
    fun deleteDownload(item: LibraryEntity) {
        viewModelScope.launch {
            dao.deleteDownloadById(item.id)
        }
    }
}
