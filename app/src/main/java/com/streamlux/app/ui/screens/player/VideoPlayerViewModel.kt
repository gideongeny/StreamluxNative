package com.streamlux.app.ui.screens.player

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.streamlux.app.utils.GenericUrlFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.media3.datasource.cache.SimpleCache

data class ServerSource(val name: String, val url: String)

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val application: Application,
    private val urlFactory: com.streamlux.app.utils.GenericUrlFactory,
    private val libraryDao: com.streamlux.app.data.local.LibraryDao,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    val simpleCache: SimpleCache
) : ViewModel() {

    val mediaType: String = savedStateHandle["type"] ?: "movie"
    private val mediaId: String = checkNotNull(savedStateHandle["id"])
    
    private val season: Int = savedStateHandle.get<Int>("season") ?: 0
    private val episode: Int = savedStateHandle.get<Int>("episode") ?: 0
    private val title: String = savedStateHandle.get<String>("title") ?: "Unknown"
    private val poster: String = savedStateHandle.get<String>("poster") ?: ""

    private val _currentServer = MutableStateFlow<ServerSource?>(null)
    val currentServer: StateFlow<ServerSource?> = _currentServer

    private val _isOfflineFile = MutableStateFlow(false)
    val isOfflineFile: StateFlow<Boolean> = _isOfflineFile

    private val _localUri = MutableStateFlow<String?>(null)
    val localUri: StateFlow<String?> = _localUri

    private val _systemDownloadId = MutableStateFlow<Long?>(null)
    val systemDownloadId: StateFlow<Long?> = _systemDownloadId

    init {
        checkOfflineStatus()
        generateSources()
        addToHistory()
    }

    private fun checkOfflineStatus() {
        viewModelScope.launch {
            try {
                val uniqueId = if (mediaType == "tv") "${mediaId}_s${season}_e${episode}" else mediaId
                val actualItem = libraryDao.getItemByMediaId(uniqueId) ?: libraryDao.getItemByMediaId(mediaId)

                if (actualItem?.downloadStatus == "completed" && actualItem.systemDownloadId != null) {
                    // PRIMARY: Use DownloadManager to get a secure content:// URI
                    // This bypasses Android Scoped Storage restrictions that block file:// URIs
                    val dm = application.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val secureUri: Uri? = dm.getUriForDownloadedFile(actualItem.systemDownloadId!!)
                    
                    if (secureUri != null) {
                        _localUri.value = secureUri.toString()
                        _systemDownloadId.value = actualItem.systemDownloadId
                        _isOfflineFile.value = true
                        android.util.Log.d("StreamLuxPlayer", "Offline: secure URI resolved via DownloadManager")
                    } else if (actualItem.localUri != null) {
                        // FALLBACK: Use stored URI (may work on older Android versions)
                        _localUri.value = actualItem.localUri
                        _systemDownloadId.value = actualItem.systemDownloadId
                        _isOfflineFile.value = true
                        android.util.Log.w("StreamLuxPlayer", "Offline: falling back to raw URI")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("StreamLuxPlayer", "Offline check failed: ${e.message}")
            }
        }
    }

    private fun addToHistory() {
        val user = auth.currentUser ?: return
        
        viewModelScope.launch {
            try {
                // Cloud Sync: continueWatching/{userId}/items/{itemId}
                val historyItem = hashMapOf(
                    "id" to mediaId,
                    "title" to title,
                    "type" to mediaType,
                    "thumbnail" to "https://image.tmdb.org/t/p/w500$poster",
                    "progress" to 0,
                    "currentTime" to 0,
                    "duration" to 0,
                    "lastWatched" to System.currentTimeMillis()
                )
                
                firestore.collection("continueWatching")
                    .document(user.uid)
                    .collection("items")
                    .document(mediaId)
                    .set(historyItem)
                    
                // Local Sync: Room for offline library access
                val existingItem = libraryDao.getItemByMediaId(mediaId)
                if (existingItem != null) {
                    libraryDao.insertItem(existingItem.copy(isHistory = true, timestamp = System.currentTimeMillis()))
                } else {
                    libraryDao.insertItem(
                        com.streamlux.app.data.local.LibraryEntity(
                            id = mediaId,
                            mediaId = mediaId,
                            mediaType = mediaType,
                            title = title,
                            posterPath = poster,
                            isHistory = true,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
                android.util.Log.d("VideoPlayerVM", "Instant History Registered: $title")
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayerVM", "History registration failed: ${e.message}")
            }
        }
    }

    fun generateSources() {
        if (mediaType == "movie" || mediaType == "tv") {
            _currentServer.value = ServerSource("Default", urlFactory.create(mediaId, season, episode, mediaType, "SERVER_A"))
        } else if (mediaType == "youtube" || mediaType == "trailer") {
            _currentServer.value = ServerSource("Trailer", urlFactory.create(mediaId, season, episode, mediaType, "YOUTUBE"))
        } else if (mediaType == "live" || mediaType == "sports") {
            try {
                // Decode the URL if it was encoded for navigation
                val decodedUrl = java.net.URLDecoder.decode(mediaId, "UTF-8")
                _currentServer.value = ServerSource("Live Stream", decodedUrl)
            } catch (e: Exception) {
                _currentServer.value = ServerSource("Live Stream", mediaId)
            }
        }
    }
}