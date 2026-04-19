package com.streamlux.app.ui.screens.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamlux.app.data.api.MusicService
import com.streamlux.app.data.model.MusicTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val musicService: MusicService,
    private val playerManager: com.streamlux.app.services.playback.MusicPlayerManager
) : ViewModel() {

    private val _trending = MutableStateFlow<List<MusicTrack>>(emptyList())
    val trending: StateFlow<List<MusicTrack>> = _trending

    private val _searchResults = MutableStateFlow<List<MusicTrack>>(emptyList())
    val searchResults: StateFlow<List<MusicTrack>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val currentTrack: StateFlow<MusicTrack?> = playerManager.currentTrack

    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying

    val genres = listOf(
        "Afrobeats", "Hip Hop Classics", "Global Pop", "R&B Hits", "Latin Reggaeton",
        "Electronic Dance", "K-Pop Top Hits", "Alternative Rock", "Chill Lo-Fi", "Country Anthems"
    )

    private val youtubeKeys = listOf(
        com.streamlux.app.utils.Constants.YOUTUBE_API_KEY_1,
        com.streamlux.app.utils.Constants.YOUTUBE_API_KEY_2,
        com.streamlux.app.utils.Constants.YOUTUBE_API_KEY_3,
        com.streamlux.app.utils.Constants.YOUTUBE_API_KEY_4
    )
    private var currentKeyIndex = 0

    init {
        loadTrending()
    }

    fun loadTrending() {
        viewModelScope.launch {
            _isLoading.value = true
            _trending.value = musicService.getTrending()
            _isLoading.value = false
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            
            // Parallel Search
            val saavnResults = musicService.search(query)
            var ytResults = emptyList<MusicTrack>()
            
            // YouTube Search with Rotation
            for (i in 0 until youtubeKeys.size) {
                val keyIndex = (currentKeyIndex + i) % youtubeKeys.size
                try {
                    ytResults = musicService.searchYouTube(query, youtubeKeys[keyIndex])
                    currentKeyIndex = keyIndex // Keep using the successful key
                    break
                } catch (e: Exception) {
                    if (e.message == "QUOTA_EXCEEDED") {
                        continue // Try next key
                    } else {
                        break // Other error
                    }
                }
            }
            
            // Merge results (YouTube first for visual confirmation, then Saavn)
            val merged = (ytResults + saavnResults).distinctBy { it.id }
            _searchResults.value = merged
            
            _isLoading.value = false
        }
    }

    fun playTrack(track: MusicTrack) {
        viewModelScope.launch {
            var trackToPlay = track
            // If YouTube source, resolve the direct stream URL if it's currently just the ID
            if (track.source == "youtube" && (track.streamUrl == null || track.streamUrl == track.id)) {
                val resolvedUrl = musicService.resolveYouTubeStream(track.id)
                if (resolvedUrl != null) {
                    trackToPlay = track.copy(streamUrl = resolvedUrl)
                }
            }
            playerManager.playTrack(trackToPlay)
        }
    }

    fun togglePlay() {
        playerManager.togglePlay()
    }
}
