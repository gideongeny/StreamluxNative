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
            
            val saavnResults = musicService.search(query)
            _searchResults.value = saavnResults.distinctBy { it.id }
            
            _isLoading.value = false
        }
    }

    fun playTrack(track: MusicTrack) {
        viewModelScope.launch {
            playerManager.playTrack(track)
        }
    }

    fun togglePlay() {
        playerManager.togglePlay()
    }
}
