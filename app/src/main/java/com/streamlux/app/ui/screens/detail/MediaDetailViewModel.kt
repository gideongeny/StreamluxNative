package com.streamlux.app.ui.screens.detail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamlux.app.data.api.VylaApi
import com.streamlux.app.data.repository.TmdbRepository
import com.streamlux.app.data.model.FilmInfo
import com.streamlux.app.data.model.VylaDownloadLink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaDetailViewModel @Inject constructor(
    private val tmdbRepository: TmdbRepository,
    private val vylaApi: VylaApi,
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val libraryDao: com.streamlux.app.data.local.LibraryDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _downloadLinks = MutableStateFlow<List<VylaDownloadLink>>(emptyList())
    val downloadLinks: StateFlow<List<VylaDownloadLink>> = _downloadLinks

    private val _vylaLoading = MutableStateFlow(false)
    val vylaLoading: StateFlow<Boolean> = _vylaLoading

    private val _vylaError = MutableStateFlow<String?>(null)
    val vylaError: StateFlow<String?> = _vylaError

    fun fetchDownloadLinks(season: Int? = null, episode: Int? = null) {
        _vylaLoading.value = true
        _vylaError.value = null
        _downloadLinks.value = emptyList()

        viewModelScope.launch {
            try {
                val response = if (mediaType == "tv") {
                    if (season != null && episode != null) {
                        vylaApi.getTvDownloads(mediaId, season, episode)
                    } else {
                        _vylaError.value = "Invalid season or episode for TV show"
                        _vylaLoading.value = false
                        return@launch
                    }
                } else {
                    vylaApi.getMovieDownloads(mediaId)
                }

                if (response.isSuccessful) {
                    val bodyString = response.body()?.string() ?: ""
                    val parsedLinks = try {
                        val jsonElement = com.google.gson.JsonParser.parseString(bodyString)
                        if (jsonElement.isJsonArray) {
                            val type = object : com.google.gson.reflect.TypeToken<List<VylaDownloadLink>>() {}.type
                            com.google.gson.Gson().fromJson<List<VylaDownloadLink>>(jsonElement, type)
                        } else if (jsonElement.isJsonObject) {
                            val obj = jsonElement.asJsonObject
                            if (obj.has("downloads")) {
                                val type = object : com.google.gson.reflect.TypeToken<List<VylaDownloadLink>>() {}.type
                                com.google.gson.Gson().fromJson<List<VylaDownloadLink>>(obj.get("downloads"), type)
                            } else {
                                emptyList()
                            }
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
                        Log.e("MediaDetailVM", "Vyla parsing error: ${e.message}")
                        emptyList()
                    }

                    // Deduplicate keeping the best quality / server combo, sort from highest quality down
                    val sorted = parsedLinks
                        .distinctBy { "${it.quality}_${it.server}" }
                        .sortedWith(compareByDescending<VylaDownloadLink> {
                            when (it.quality.lowercase()) {
                                "4k", "2160p" -> 4
                                "1080p" -> 3
                                "720p" -> 2
                                "480p" -> 1
                                else -> 0
                            }
                        }.thenBy { it.server })

                    _downloadLinks.value = sorted
                } else {
                    _vylaError.value = "Failed to fetch download links: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("MediaDetailVM", "Vyla fetch error: ${e.message}")
                _vylaError.value = e.message ?: "Unknown network error"
            } finally {
                _vylaLoading.value = false
            }
        }
    }

    val mediaType: String = savedStateHandle["type"] ?: "movie"
    val mediaId: String = savedStateHandle["id"] ?: ""

    private val _filmInfo = MutableStateFlow<FilmInfo?>(null)
    val filmInfo: StateFlow<FilmInfo?> = _filmInfo

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError
    
    private val _selectedSeason = MutableStateFlow(1)
    val selectedSeason: StateFlow<Int> = _selectedSeason

    private val _seasonEpisodes = MutableStateFlow<List<com.streamlux.app.data.model.Episode>>(emptyList())
    val seasonEpisodes: StateFlow<List<com.streamlux.app.data.model.Episode>> = _seasonEpisodes

    private val _comments = MutableStateFlow<List<com.streamlux.app.data.model.Comment>>(emptyList())
    val comments: StateFlow<List<com.streamlux.app.data.model.Comment>> = _comments
    
    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked

    private val _downloadedItem = MutableStateFlow<com.streamlux.app.data.local.LibraryEntity?>(null)
    val downloadedItem: StateFlow<com.streamlux.app.data.local.LibraryEntity?> = _downloadedItem

    private val _downloadedEpisodes = MutableStateFlow<List<com.streamlux.app.data.local.LibraryEntity>>(emptyList())
    val downloadedEpisodes: StateFlow<List<com.streamlux.app.data.local.LibraryEntity>> = _downloadedEpisodes

    init {
        if (mediaId.isNotBlank()) {
            fetchFullDetail()
            fetchComments()
            checkBookmarkStatus()
            checkDownloadStatus()
        } else {
            _loadError.value = "Invalid title ID"
        }
    }

    fun retryDetail() {
        _loadError.value = null
        _filmInfo.value = null
        fetchFullDetail()
    }

    private fun checkBookmarkStatus() {
        viewModelScope.launch {
            try {
                val item = libraryDao.getItemByMediaId(mediaId)
                _isBookmarked.value = item?.isWatchlist == true
            } catch (e: Exception) {
                Log.e("MediaDetailVM", "Error checking bookmark: ${e.message}")
            }
        }
    }

    private fun checkDownloadStatus() {
        viewModelScope.launch {
            try {
                // Tracking all episodes for ticks on individual episode list
                libraryDao.getEpisodesForShow(mediaId).collect { episodes ->
                    _downloadedEpisodes.value = episodes
                    
                    // Tracks the first downloaded episode of the currently selected season
                    // to drive the "WATCH OFFLINE" play button. Previously hardcoded to
                    // episodeNumber == 1, which caused downloaded S1E12 to show as "not downloaded".
                    _downloadedItem.value = episodes.find {
                        if (mediaType == "movie") true
                        else it.seasonNumber == _selectedSeason.value
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaDetailVM", "Error checking download status: ${e.message}")
            }
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
                        id = mediaId,
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
                val current = libraryDao.getItemByMediaId(mediaId)
                if (current != null) {
                    libraryDao.insertItem(current.copy(isWatchlist = _isBookmarked.value))
                } else {
                    libraryDao.insertItem(
                        com.streamlux.app.data.local.LibraryEntity(
                            id = mediaId,
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

    /** Keeps the title in the local library even when CDN links are unavailable. */
    fun registerDownloadIntent(
        season: Int? = null,
        episode: Int? = null,
        episodeName: String? = null,
        episodeStillPath: String? = null
    ) {
        val info = _filmInfo.value?.detail ?: return
        viewModelScope.launch {
            try {
                val uniqueId = if (mediaType == "tv" && season != null && episode != null) {
                    "${mediaId}_s${season}_e${episode}"
                } else {
                    mediaId
                }

                val existing = libraryDao.getItemByMediaId(uniqueId)
                if (existing?.downloadStatus == "completed") return@launch

                libraryDao.insertItem(
                    com.streamlux.app.data.local.LibraryEntity(
                        id = uniqueId,
                        mediaId = mediaId,
                        mediaType = mediaType,
                        title = if (mediaType == "tv" && season != null && episode != null) {
                            "${info.displayTitle} S${season} E${episode}"
                        } else {
                            info.displayTitle
                        },
                        posterPath = info.posterPath,
                        isDownload = true,
                        downloadStatus = "pending",
                        downloadProgress = 0,
                        parentId = if (mediaType == "tv") mediaId else null,
                        seriesTitle = if (mediaType == "tv") info.displayTitle else null,
                        seasonNumber = season,
                        episodeNumber = episode,
                        episodeName = episodeName,
                        episodeStillPath = episodeStillPath,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e("MediaDetailVM", "Failed to register download intent: ${e.message}")
            }
        }
    }

    /** Called when the user starts a download from the WebView portal */
    fun onDownloadStarted(
        systemDownloadId: Long, 
        quality: String, 
        season: Int? = null, 
        episode: Int? = null,
        episodeName: String? = null,
        episodeStillPath: String? = null
    ) {
        val info = _filmInfo.value?.detail ?: return
        viewModelScope.launch {
            try {
                val uniqueId = if (mediaType == "tv" && season != null && episode != null) {
                    "${mediaId}_s${season}_e${episode}"
                } else {
                    mediaId
                }

                val downloadItem = com.streamlux.app.data.local.LibraryEntity(
                    id = uniqueId,
                    mediaId = mediaId,
                    mediaType = mediaType,
                    title = if (mediaType == "tv") "${info.displayTitle} S${season} E${episode}" else info.displayTitle,
                    posterPath = info.posterPath,
                    isDownload = true,
                    downloadStatus = "downloading",
                    downloadProgress = 0,
                    downloadQuality = quality,
                    systemDownloadId = systemDownloadId,
                    parentId = if (mediaType == "tv") mediaId else null,
                    seriesTitle = if (mediaType == "tv") info.displayTitle else null,
                    seasonNumber = season,
                    episodeNumber = episode,
                    episodeName = episodeName,
                    episodeStillPath = episodeStillPath,
                    timestamp = System.currentTimeMillis()
                )
                libraryDao.insertItem(downloadItem)
            } catch (e: Exception) {
                Log.e("MediaDetailVM", "Failed to record download: ${e.message}")
            }
        }
    }

    private fun fetchFullDetail() {
        viewModelScope.launch {
            try {
                val detailDef = async { tmdbRepository.fetchDetail("/$mediaType/$mediaId") }
                val creditsDef = async {
                    tmdbRepository.fetchCredits("/$mediaType/$mediaId/credits")
                }
                val similarDef = async {
                    tmdbRepository.fetch("/$mediaType/$mediaId/similar").results
                }
                val videosDef = async {
                    tmdbRepository.fetchVideos("/$mediaType/$mediaId/videos")
                }

                val detail = try {
                    detailDef.await()
                } catch (e: Exception) {
                    Log.e("MediaDetailVM", "CRITICAL - Detailed fetch failed: ${e.message}")
                    _loadError.value = "Could not load details. Check connection and retry."
                    return@launch
                }

                // Show details immediately while waiting for credits/similar/videos
                _filmInfo.value = FilmInfo(
                    detail = detail,
                    credits = emptyList(),
                    similar = emptyList(),
                    trailerKey = null
                )

                val castList = creditsDef.await()?.cast?.take(8) ?: emptyList()
                val similarList = similarDef.await() ?: emptyList()
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
                val res = tmdbRepository.fetchSeasonDetail("/tv/$mediaId/season/$seasonNum")
                _seasonEpisodes.value = res?.episodes ?: emptyList()
                
                // Refresh download status for the new season's first episode
                checkDownloadStatus()
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
