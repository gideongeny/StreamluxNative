package com.streamlux.app.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamlux.app.data.api.SerpApiService
import com.streamlux.app.data.repository.TmdbRepository
import com.streamlux.app.data.model.HomeSection
import com.streamlux.app.data.model.ShortVideoItem
import com.streamlux.app.data.model.TmdbItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tmdbRepository: TmdbRepository,
    private val serpApiService: SerpApiService,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val _moviesData = MutableStateFlow<List<HomeSection>>(emptyList())
    val moviesData: StateFlow<List<HomeSection>> = _moviesData

    private val _tvData = MutableStateFlow<List<HomeSection>>(emptyList())
    val tvData: StateFlow<List<HomeSection>> = _tvData

    private val _shortsData = MutableStateFlow<List<ShortVideoItem>>(emptyList())
    val shortsData: StateFlow<List<ShortVideoItem>> = _shortsData

    private var moviesLoaded = false
    private var tvLoaded = false

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError

    companion object {
        private val COLLECTION_IDS = listOf(
            86311, 1241, 10, 9485, 119, 295130, 10194, 748, 404609
        )
    }

    init {
        _currentUser.value = auth.currentUser
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
        fetchMovies()
        fetchTvShows()
        fetchShortVideos("trending movie trailers shorts")
    }

    private fun fetchShortVideos(query: String) {
        viewModelScope.launch {
            try {
                val response = serpApiService.getShortVideos(query = query)
                _shortsData.value = response.shortVideos
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching shorts", e)
            }
        }
    }

    private suspend fun fetchSection(title: String, path: String): HomeSection {
        return try {
            val response = tmdbRepository.fetch(path)
            HomeSection(title, response.results ?: emptyList())
        } catch (e: Exception) {
            HomeSection(title, emptyList())
        }
    }

    private suspend fun fetchTop20(mediaType: String): HomeSection {
        val path = if (mediaType == "movie") "/trending/movie/day" else "/trending/tv/day"
        return try {
            val page1 = tmdbRepository.fetch(path)
            val page2 = tmdbRepository.fetch("$path?page=2")
            val merged = mutableListOf<TmdbItem>()
            val seen = mutableSetOf<Int>()
            (page1.results.orEmpty() + page2.results.orEmpty()).forEach { item ->
                if (seen.add(item.id)) merged.add(item)
            }
            HomeSection("⚡ TOP 20 Today", merged.take(20))
        } catch (e: Exception) {
            HomeSection("⚡ TOP 20 Today", emptyList())
        }
    }

    private suspend fun fetchCollections(): HomeSection {
        val items = coroutineScope {
            COLLECTION_IDS.map { id ->
                async {
                    try {
                        val col = tmdbRepository.fetchDetail("/collection/$id")
                        if (col.posterPath != null || col.backdropPath != null) {
                            col.copy(
                                title = col.displayTitle,
                                mediaType = "collection"
                            )
                        } else null
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }
        return HomeSection("🎬 Epic Collections", items)
    }

    private suspend fun fetchBatched(
        requests: List<Pair<String, String>>,
        batchSize: Int = 4,
        onPartial: ((List<HomeSection>) -> Unit)? = null
    ): List<HomeSection> {
        val results = mutableListOf<HomeSection>()
        for (chunk in requests.chunked(batchSize)) {
            val batch = coroutineScope {
                chunk.map { (title, path) ->
                    async { fetchSection(title, path) }
                }.awaitAll()
            }
            results.addAll(batch.filter { it.items.isNotEmpty() })
            if (results.isNotEmpty()) {
                onPartial?.invoke(results.toList())
            }
            // delay(120) removed for faster loading
        }
        return results
    }

    fun retryLoad() {
        moviesLoaded = false
        tvLoaded = false
        _moviesData.value = emptyList()
        _tvData.value = emptyList()
        fetchMovies()
        fetchTvShows()
    }

    private fun fetchMovies() {
        if (moviesLoaded) return
        viewModelScope.launch {
            _isLoading.value = true
            _loadError.value = null
            try {
                val fastRequests = listOf(
                    "🔥 Trending" to "/trending/movie/day",
                    "🌟 Popular" to "/movie/popular",
                    "⭐ Top Rated" to "/movie/top_rated",
                    "🆕 Now Playing" to "/movie/now_playing",
                    "📅 Upcoming" to "/movie/upcoming"
                )
                val top20 = async { fetchTop20("movie") }
                val collections = async { fetchCollections() }

                val fastResults = fetchBatched(fastRequests, batchSize = 6) { partial ->
                    if (partial.isNotEmpty()) _moviesData.value = partial
                }
                _isLoading.value = false // Hide spinner as soon as first batch arrives

                val genreRequests = listOf(
                    "💥 Action" to "/discover/movie?with_genres=28",
                    "😂 Comedy" to "/discover/movie?with_genres=35",
                    "😱 Horror" to "/discover/movie?with_genres=27",
                    "🚀 Sci-Fi" to "/discover/movie?with_genres=878",
                    "🎭 Drama" to "/discover/movie?with_genres=18",
                    "🎡 Animation" to "/discover/movie?with_genres=16",
                    "🧩 Thriller" to "/discover/movie?with_genres=53",
                    "❤️ Romance" to "/discover/movie?with_genres=10749",
                    "🔥 Hot This Week" to "/trending/movie/week"
                )
                val top20Section = top20.await()
                val collectionsSection = collections.await()
                val genreResults = fetchBatched(genreRequests, batchSize = 5) { partial ->
                    mergeMovieSections(fastResults, top20Section, collectionsSection, partial)
                }

                val combined = mergeMovieSections(fastResults, top20Section, collectionsSection, genreResults)
                moviesLoaded = combined.isNotEmpty()
                if (combined.isEmpty()) _loadError.value = "Could not load movies. Tap retry."
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching Movies", e)
                _loadError.value = "Network error loading movies."
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchTvShows() {
        if (tvLoaded) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fastRequests = listOf(
                    "🔥 Trending" to "/trending/tv/day",
                    "🌟 Popular" to "/tv/popular",
                    "⭐ Top Rated" to "/tv/top_rated",
                    "📡 On The Air" to "/tv/on_the_air",
                    "⏰ Airing Today" to "/tv/airing_today"
                )
                val top20 = async { fetchTop20("tv") }

                val fastResults = fetchBatched(fastRequests, batchSize = 6) { partial ->
                    if (partial.isNotEmpty()) _tvData.value = partial
                }
                _isLoading.value = false // Hide spinner early

                val genreRequests = listOf(
                    "💥 Action & Adventure" to "/discover/tv?with_genres=10759",
                    "😂 Comedy" to "/discover/tv?with_genres=35",
                    "🎭 Drama" to "/discover/tv?with_genres=18",
                    "🚀 Sci-Fi & Fantasy" to "/discover/tv?with_genres=10765",
                    "🌏 Korean Dramas" to "/discover/tv?with_origin_country=KR",
                    "🔥 Hot This Week" to "/trending/tv/week"
                )
                val top20Section = top20.await()
                val genreResults = fetchBatched(genreRequests, batchSize = 5) { partial ->
                    mergeTvSections(fastResults, top20Section, partial)
                }

                val combined = mergeTvSections(fastResults, top20Section, genreResults)
                tvLoaded = combined.isNotEmpty()
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching TV Shows", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun mergeMovieSections(
        fast: List<HomeSection>,
        top20: HomeSection,
        collections: HomeSection,
        genres: List<HomeSection>
    ): List<HomeSection> {
        val combined = (fast + listOf(top20, collections) + genres)
            .filter { it.items.isNotEmpty() }
            .distinctBy { it.title }
        if (combined.isNotEmpty()) _moviesData.value = combined
        return combined
    }

    private fun mergeTvSections(
        fast: List<HomeSection>,
        top20: HomeSection,
        genres: List<HomeSection>
    ): List<HomeSection> {
        val combined = (fast + listOf(top20) + genres)
            .filter { it.items.isNotEmpty() }
            .distinctBy { it.title }
        if (combined.isNotEmpty()) _tvData.value = combined
        return combined
    }

    fun prefetchMedia(id: Int, mediaType: String) {
        viewModelScope.launch {
            try {
                tmdbRepository.fetchDetail("/$mediaType/$id")
            } catch (_: Exception) {
            }
        }
    }
}
