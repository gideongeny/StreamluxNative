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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

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

    private val _activeCountry = MutableStateFlow("All")
    val activeCountry: StateFlow<String> = _activeCountry
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _categories = MutableStateFlow<List<String>>(listOf("All"))
    val categories: StateFlow<List<String>> = _categories

    private val _countries = MutableStateFlow<List<String>>(listOf("All"))
    val countries: StateFlow<List<String>> = _countries

    // ELITE PERFORMANCE: Pre-filtered and combined state for the UI
    val filteredChannels: StateFlow<List<TVChannel>> = combine(
        _allChannels, _activeCategory, _activeCountry, _searchQuery
    ) { all, category, country, query ->
        all.filter { channel ->
            val matchesCategory = category == "All" || channel.category == category
            val matchesCountry = country == "All" || (channel.country ?: "Global") == country
            val matchesSearch = query.isEmpty() || channel.name.contains(query, ignoreCase = true)
            matchesCategory && matchesCountry && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            try {
                val channels = tvChannelRepository.getLiveChannels()
                _allChannels.value = channels
                _categories.value = listOf("All") + channels.map { it.category }.distinct().sorted()
                updateCountries(channels, "All")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateCountries(channels: List<TVChannel>, category: String) {
        val available = if (category == "All") channels else channels.filter { it.category == category }
        _countries.value = listOf("All") + available.map { it.country ?: "Global" }.distinct().sorted()
    }

    fun setCategory(category: String) {
        _activeCategory.value = category
        _activeCountry.value = "All"
        updateCountries(_allChannels.value, category)
    }

    fun setCountry(country: String) {
        _activeCountry.value = country
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
