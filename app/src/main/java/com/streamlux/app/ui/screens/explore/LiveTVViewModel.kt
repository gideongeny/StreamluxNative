package com.streamlux.app.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamlux.app.data.model.TVChannel
import com.streamlux.app.data.repository.TvChannelRepository
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LiveTVViewModel @Inject constructor(
    private val tvChannelRepository: TvChannelRepository
) : ViewModel() {
    
    private val _allChannels = MutableStateFlow<List<TVChannel>>(emptyList())
    val allChannels: StateFlow<List<TVChannel>> = _allChannels

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _activeCategory = MutableStateFlow("All")
    val activeCategory: StateFlow<String> = _activeCategory
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _categories = MutableStateFlow<List<String>>(listOf("All"))
    val categories: StateFlow<List<String>> = _categories

    init {
        viewModelScope.launch {
            // Fetch channels dynamically from Firebase Hosting
            val channels = tvChannelRepository.getLiveChannels()
            _allChannels.value = channels
            _categories.value = listOf("All") + channels.map { it.category }.distinct()
            _isLoading.value = false
        }
    }

    fun setCategory(category: String) {
        _activeCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
