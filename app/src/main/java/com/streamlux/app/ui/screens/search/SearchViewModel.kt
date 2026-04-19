package com.streamlux.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamlux.app.data.api.TmdbApi
import com.streamlux.app.data.model.TmdbItem
import com.streamlux.app.data.model.TVChannel
import com.streamlux.app.data.repository.TvChannelRepository
import com.streamlux.app.data.model.MusicTrack
import com.streamlux.app.data.api.StreamLuxApi
import com.google.gson.JsonElement
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val streamLuxApi: StreamLuxApi,
    private val tvChannelRepository: TvChannelRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<SearchState>(SearchState.Empty)
    val searchResults: StateFlow<SearchState> = _searchResults
    private val queryCache = linkedMapOf<String, SearchState.Results>()
    private var lastQuery: String? = null

    fun search(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            _searchResults.value = SearchState.Empty
            lastQuery = null
            return
        }

        if (lastQuery == normalizedQuery && _searchResults.value is SearchState.Results) {
            return
        }

        queryCache[normalizedQuery]?.let { cached ->
            lastQuery = normalizedQuery
            _searchResults.value = cached
            return
        }

        viewModelScope.launch {
            _searchResults.value = SearchState.Loading
            try {
                // 1. Search Movies/TV on TMDB (via the generic vertex/api/tmdb endpoint)
                val encodedQuery = java.net.URLEncoder.encode(normalizedQuery, "UTF-8")
                val movies = tmdbApi.fetch("search/movie?query=$encodedQuery").results ?: emptyList()
                val tvShows = tmdbApi.fetch("search/tv?query=$encodedQuery").results ?: emptyList()
                
                // 2. Search Live TV Channels (Cloud DB)
                val channels = tvChannelRepository.getLiveChannels().filter { 
                    it.name.contains(normalizedQuery, ignoreCase = true) || it.category.contains(normalizedQuery, ignoreCase = true)
                }

                // 3. Search Music (StreamLux API)
                val musicRes = try { streamLuxApi.searchMusic(normalizedQuery) } catch(e:Exception) { null }
                val tracks = extractMusic(musicRes)

                val results = SearchState.Results(
                    movies = movies,
                    tvShows = tvShows,
                    channels = channels,
                    music = tracks
                )
                lastQuery = normalizedQuery
                queryCache[normalizedQuery] = results
                while (queryCache.size > 20) {
                    queryCache.remove(queryCache.entries.first().key)
                }
                _searchResults.value = results
            } catch (e: Exception) {
                _searchResults.value = SearchState.Error(e.localizedMessage ?: "Search failed")
            }
        }
    }

    private fun extractMusic(json: JsonElement?): List<MusicTrack> {
        if (json == null) return emptyList()
        val list = mutableListOf<MusicTrack>()
        try {
            val root = if (json.isJsonObject) json.asJsonObject else return emptyList()
            val array = when {
                root.has("items") && root.get("items").isJsonArray -> root.getAsJsonArray("items")
                root.has("songs") && root.get("songs").isJsonArray -> root.getAsJsonArray("songs")
                root.has("data") && root.get("data").isJsonArray -> root.getAsJsonArray("data")
                else -> null
            }
            array?.forEach { element ->
                if (element.isJsonObject) {
                    val obj = element.asJsonObject
                    if (obj.has("snippet")) {
                        // YouTube style
                        val snip = obj.getAsJsonObject("snippet")
                        val id = obj.getAsJsonObject("id")?.get("videoId")?.asString ?: ""
                                                list.add(MusicTrack(
                            id = id,
                            title = snip.get("title")?.asString ?: "",
                            artist = snip.get("channelTitle")?.asString ?: "",
                            album = null,
                            duration = null,
                            thumbnail = snip.getAsJsonObject("thumbnails")?.getAsJsonObject("medium")?.get("url")?.asString ?: "",
                            streamUrl = null,
                            source = "youtube"
                        ))
                    } else {
                        // Saavn style
                        val id = obj.get("id")?.asString ?: ""
                        val title = obj.get("name")?.asString ?: obj.get("title")?.asString ?: ""
                                                list.add(MusicTrack(
                            id = id,
                            title = title,
                            artist = "",
                            album = null,
                            duration = null,
                            thumbnail = "",
                            streamUrl = null,
                            source = "saavn"
                        ))
                    }
                }
            }
        } catch (e: Exception) {}
        return list
    }
}

sealed class SearchState {
    object Empty : SearchState()
    object Loading : SearchState()
    data class Results(
        val movies: List<TmdbItem>,
        val tvShows: List<TmdbItem>,
        val channels: List<TVChannel>,
        val music: List<MusicTrack>
    ) : SearchState()
    data class Error(val message: String) : SearchState()
}
