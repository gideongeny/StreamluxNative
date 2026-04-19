package com.streamlux.app.ui.screens.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.streamlux.app.utils.GenericUrlFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerSource(val name: String, val url: String)

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val urlFactory: com.streamlux.app.utils.GenericUrlFactory,
    private val libraryDao: com.streamlux.app.data.local.LibraryDao,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val auth: com.google.firebase.auth.FirebaseAuth
) : ViewModel() {

    val mediaType: String = savedStateHandle["type"] ?: "movie"
    private val mediaId: String = checkNotNull(savedStateHandle["id"])
    
    private val season: Int = savedStateHandle.get<Int>("season") ?: 0
    private val episode: Int = savedStateHandle.get<Int>("episode") ?: 0
    private val title: String = savedStateHandle.get<String>("title") ?: "Unknown"
    private val poster: String = savedStateHandle.get<String>("poster") ?: ""

    private val _currentServer = MutableStateFlow<ServerSource?>(null)
    val currentServer: StateFlow<ServerSource?> = _currentServer

    init {
        generateSources()
        addToHistory()
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
                libraryDao.insertItem(
                    com.streamlux.app.data.local.LibraryEntity(
                        mediaId = mediaId,
                        mediaType = mediaType,
                        title = title,
                        posterPath = poster,
                        isHistory = true,
                        timestamp = System.currentTimeMillis()
                    )
                )
                android.util.Log.d("VideoPlayerVM", "Instant History Registered: $title")
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayerVM", "History registration failed: ${e.message}")
            }
        }
    }

    private fun generateSources() {
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