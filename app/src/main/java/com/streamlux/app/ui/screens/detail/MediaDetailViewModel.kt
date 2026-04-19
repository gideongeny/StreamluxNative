package com.streamlux.app.ui.screens.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamlux.app.data.api.TmdbApi
import com.streamlux.app.data.model.FilmInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaDetailViewModel @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val libraryDao: com.streamlux.app.data.local.LibraryDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Retrieve arguments from Navigation Compose route
    val mediaType: String = checkNotNull(savedStateHandle["type"])
    val mediaId: String = checkNotNull(savedStateHandle["id"])

    private val _filmInfo = MutableStateFlow<FilmInfo?>(null)
    val filmInfo: StateFlow<FilmInfo?> = _filmInfo
    
    private val _selectedSeason = MutableStateFlow(1)
    val selectedSeason: StateFlow<Int> = _selectedSeason

    private val _seasonEpisodes = MutableStateFlow<List<com.streamlux.app.data.model.Episode>>(emptyList())
    val seasonEpisodes: StateFlow<List<com.streamlux.app.data.model.Episode>> = _seasonEpisodes

    private val _comments = MutableStateFlow<List<com.streamlux.app.data.model.Comment>>(emptyList())
    val comments: StateFlow<List<com.streamlux.app.data.model.Comment>> = _comments
    
    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked

    init {
        fetchFullDetail()
        fetchComments()
        checkBookmarkStatus()
        addToHistory()
    }

    private fun checkBookmarkStatus() {
        viewModelScope.launch {
            val item = libraryDao.getItemById(mediaId)
            _isBookmarked.value = item?.isWatchlist == true
        }
    }

    private fun addToHistory() {
        val user = auth.currentUser ?: return
        val info = _filmInfo.value?.detail ?: return
        
        viewModelScope.launch {
            try {
                // Parity with website schema: continueWatching/{userId}/items/{itemId}
                val historyItem = hashMapOf(
                    "id" to mediaId,
                    "title" to info.displayTitle,
                    "type" to mediaType,
                    "thumbnail" to "https://image.tmdb.org/t/p/w500${info.posterPath}",
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
                    
                // Also update local Room for offline access
                libraryDao.insertItem(
                    com.streamlux.app.data.local.LibraryEntity(
                        mediaId = mediaId,
                        mediaType = mediaType,
                        title = info.displayTitle,
                        posterPath = info.posterPath,
                        isHistory = true,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e("MediaDetailVM", "Firestore history sync failed: ${e.message}")
            }
        }
    }
    
    fun toggleBookmark() {
        val user = auth.currentUser ?: return
        val info = _filmInfo.value?.detail ?: return
        
        viewModelScope.launch {
            try {
                val bookmarkItem = hashMapOf(
                    "id" to mediaId,
                    "poster_path" to info.posterPath,
                    "vote_average" to info.voteAverage,
                    "media_type" to mediaType,
                    "title" to info.displayTitle
                )
                
                val userRef = firestore.collection("users").document(user.uid)
                
                if (_isBookmarked.value) {
                    userRef.update("bookmarks", com.google.firebase.firestore.FieldValue.arrayRemove(bookmarkItem))
                } else {
                    userRef.update("bookmarks", com.google.firebase.firestore.FieldValue.arrayUnion(bookmarkItem))
                }
                
                _isBookmarked.value = !_isBookmarked.value
                
                // Update local Room
                val current = libraryDao.getItemById(mediaId)
                if (current != null) {
                    libraryDao.insertItem(current.copy(isWatchlist = _isBookmarked.value))
                } else {
                    libraryDao.insertItem(
                        com.streamlux.app.data.local.LibraryEntity(
                            mediaId = mediaId,
                            mediaType = mediaType,
                            title = info.displayTitle,
                            posterPath = info.posterPath,
                            isWatchlist = _isBookmarked.value
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("MediaDetailVM", "Firestore bookmark sync failed: ${e.message}")
            }
        }
    }

    private fun fetchFullDetail() {
        viewModelScope.launch {
            try {
                // Emulate Promise.all dynamically with better error handling for each deferrable
                val detailDef = async { tmdbApi.fetchDetail("/$mediaType/$mediaId") }
                val creditsDef = async { 
                    try { tmdbApi.fetchCredits("/$mediaType/$mediaId/credits") } catch(e:Exception){ 
                        Log.e("MediaDetailVM", "Credits fetch failed: ${e.message}")
                        null 
                    } 
                }
                val similarDef = async { 
                    try { tmdbApi.fetch("/$mediaType/$mediaId/similar") } catch(e:Exception){ 
                        Log.e("MediaDetailVM", "Similar fetch failed: ${e.message}")
                        null 
                    } 
                }
                val videosDef = async { 
                    try { tmdbApi.fetchVideos("/$mediaType/$mediaId/videos") } catch(e:Exception){ 
                        Log.e("MediaDetailVM", "Videos fetch failed: ${e.message}")
                        null 
                    } 
                }

                val detail = try { detailDef.await() } catch (e: Exception) {
                    Log.e("MediaDetailVM", "CRITICAL - Detailed fetch failed: ${e.message}")
                    return@launch
                }
                
                val castList = creditsDef.await()?.cast?.take(8) ?: emptyList()
                val similarList = similarDef.await()?.results ?: emptyList()
                val videosList = videosDef.await()?.results?.filter { it.site == "YouTube" } ?: emptyList()
                
                // Prefer trailer, else grab any from TMDB
                val trailerKey = videosList.firstOrNull { it.type == "Trailer" }?.key ?: videosList.firstOrNull()?.key
                
                _filmInfo.value = FilmInfo(
                    detail = detail,
                    credits = castList,
                    similar = similarList,
                    trailerKey = trailerKey
                )
                
                if (mediaType == "tv" && !detail.seasons.isNullOrEmpty()) {
                    val firstRealSeason = detail.seasons.find { it.seasonNumber > 0 }?.seasonNumber ?: 1
                    selectSeason(firstRealSeason)
                }
                
                // Add to History
                addToHistory()

            } catch (e: Exception) {
                Log.e("MediaDetailViewModel", "Error fetching full detail for $mediaType/$mediaId", e)
            }
        }
    }
    
    fun selectSeason(seasonNum: Int) {
        _selectedSeason.value = seasonNum
        viewModelScope.launch {
            try {
                val res = tmdbApi.fetchSeasonDetail("/tv/$mediaId/season/$seasonNum")
                _seasonEpisodes.value = res.episodes ?: emptyList()
            } catch (e: Exception) {
                Log.e("MediaDetailViewModel", "Error fetching season $seasonNum", e)
                _seasonEpisodes.value = emptyList()
            }
        }
    }

    private fun fetchComments() {
        try {
            firestore.collection("comments")
                .whereEqualTo("mediaId", mediaId)
                // Removed orderBy to avoid index requirement, sorting locally for reliability
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("MediaDetailVM", "Firestore comments error: ${e.message}")
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { doc ->
                        try { 
                            doc.toObject(com.streamlux.app.data.model.Comment::class.java) 
                        } catch (ex: Exception) {
                            Log.e("MediaDetailVM", "Error parsing comment: ${ex.message}")
                            null
                        }
                    } ?: emptyList()
                    
                    // Sort locally by timestamp DESC
                    _comments.value = list.sortedByDescending { it.timestamp }
                }
        } catch (e: Exception) {
            Log.e("MediaDetailVM", "Failed to setup comments listener: ${e.message}")
        }
    }

    fun postComment(content: String, rating: Float) {
        val user = auth.currentUser ?: return
        val comment = hashMapOf(
            "mediaId" to mediaId,
            "userId" to user.uid,
            "userName" to (user.displayName ?: "User"),
            "content" to content,
            "rating" to rating,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("comments").add(comment)
    }

    fun isUserLoggedIn() = auth.currentUser != null
}
