package com.streamlux.app.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamlux.app.data.api.SerpApiService
import com.streamlux.app.data.api.TmdbApi
import com.streamlux.app.data.model.HomeSection
import com.streamlux.app.data.model.ShortVideoItem
import com.streamlux.app.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tmdbApi: TmdbApi,
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
                val response = serpApiService.getShortVideos(
                    query = query
                )
                _shortsData.value = response.shortVideos
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching shorts", e)
            }
        }
    }

    private fun fetchMovies() {
        if (moviesLoaded) return
        viewModelScope.launch {
            try {
                // Phase 1: Fast load — core TMDB sections (matches website home.ts)
                val fastRequests = listOf(
                    "🔥 Trending"                to "/trending/movie/day",
                    "🌟 Popular"                  to "/movie/popular",
                    "⭐ Top Rated"                to "/movie/top_rated",
                    "🆕 Now Playing"              to "/movie/now_playing",
                    "📅 Upcoming"                 to "/movie/upcoming"
                )
                val fastResults = fastRequests.map { (title, path) ->
                    async {
                        try {
                            val response = tmdbApi.fetch(path)
                            HomeSection(title, response.results ?: emptyList())
                        } catch (e: Exception) { HomeSection(title, emptyList()) }
                    }
                }.awaitAll()
                _moviesData.value = fastResults.filter { it.items.isNotEmpty() }

                // Phase 2: Genre & special categories (matches website categories)
                val genreRequests = listOf(
                    "💥 Action"                   to "/discover/movie?with_genres=28",
                    "😂 Comedy"                   to "/discover/movie?with_genres=35",
                    "😱 Horror"                   to "/discover/movie?with_genres=27",
                    "🚀 Sci-Fi"                   to "/discover/movie?with_genres=878",
                    "🎭 Drama"                    to "/discover/movie?with_genres=18",
                    "🎡 Animation"                to "/discover/movie?with_genres=16",
                    "🧩 Thriller"                 to "/discover/movie?with_genres=53",
                    "🔍 Mystery"                  to "/discover/movie?with_genres=9648",
                    "🏰 Fantasy"                  to "/discover/movie?with_genres=14",
                    "⚔️ Adventure"               to "/discover/movie?with_genres=12",
                    "❤️ Romance"                 to "/discover/movie?with_genres=10749",
                    "💀 Crime"                   to "/discover/movie?with_genres=80",
                    "📜 History"                  to "/discover/movie?with_genres=36",
                    "🎤 Music"                    to "/discover/movie?with_genres=10402",
                    "📖 Documentary"              to "/discover/movie?with_genres=99",
                    "👨‍👩‍👧 Family"            to "/discover/movie?with_genres=10751",
                    "🌍 International"            to "/discover/movie?with_languages=fr",
                    "🦸 Marvel Universe"          to "/discover/movie?with_companies=420",
                    "🦇 DC Universe"              to "/discover/movie?with_companies=9993",
                    "⚡ TOP 10 Today"             to "/trending/movie/day?page=2",
                    "🎬 Epic Collections"         to "/movie/top_rated?page=2",
                    "🍿 Award Winners"            to "/discover/movie?sort_by=vote_average.desc&vote_count.gte=1000",
                    "🌑 Dark Thrillers"           to "/discover/movie?with_genres=53,27",
                    "🌅 Feel Good"                to "/discover/movie?with_genres=35,10749",
                    "💎 Modern Classics"          to "/discover/movie?sort_by=vote_average.desc&primary_release_date.gte=2010-01-01",
                    "🔥 Hot This Week"            to "/trending/movie/week",
                    "🌏 Kenyan Cinema"            to "/discover/movie?with_origin_country=KE",
                    "🌍 African Cinema"           to "/discover/movie?with_origin_country=NG",
                    "🎌 Anime Films"              to "/discover/movie?with_genres=16&with_origin_country=JP",
                    "🎭 Bollywood"                to "/discover/movie?with_origin_country=IN",
                    "📺 Based On TV"              to "/discover/movie?with_genres=18&sort_by=popularity.desc"
                )

                val genreResults = genreRequests.map { (title, path) ->
                    async {
                        try {
                            val response = tmdbApi.fetch(path)
                            HomeSection(title, response.results ?: emptyList())
                        } catch (e: Exception) { HomeSection(title, emptyList()) }
                    }
                }.awaitAll()

                val combined = (fastResults + genreResults).filter { it.items.isNotEmpty() }
                _moviesData.value = combined
                if (combined.isNotEmpty()) {
                    moviesLoaded = true
                }

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching Movies", e)
            }
        }
    }

    private fun fetchTvShows() {
        if (tvLoaded) return
        viewModelScope.launch {
            try {
                // Phase 1: Fast load
                val fastRequests = listOf(
                    "🔥 Trending"                to "/trending/tv/day",
                    "🌟 Popular"                  to "/tv/popular",
                    "⭐ Top Rated"                to "/tv/top_rated",
                    "📡 On The Air"               to "/tv/on_the_air",
                    "⏰ Airing Today"             to "/tv/airing_today"
                )
                val fastResults = fastRequests.map { (title, path) ->
                    async {
                        try {
                            val response = tmdbApi.fetch(path)
                            HomeSection(title, response.results ?: emptyList())
                        } catch (e: Exception) { HomeSection(title, emptyList()) }
                    }
                }.awaitAll()
                _tvData.value = fastResults.filter { it.items.isNotEmpty() }

                // Phase 2: Genre categories
                val genreRequests = listOf(
                    "💥 Action & Adventure"       to "/discover/tv?with_genres=10759",
                    "😂 Comedy"                   to "/discover/tv?with_genres=35",
                    "🎭 Drama"                    to "/discover/tv?with_genres=18",
                    "🕵️ Crime"                  to "/discover/tv?with_genres=80",
                    "😱 Horror"                   to "/discover/tv?with_genres=9648",
                    "🚀 Sci-Fi & Fantasy"         to "/discover/tv?with_genres=10765",
                    "📖 Documentary"              to "/discover/tv?with_genres=99",
                    "👨‍👩‍👧 Family"            to "/discover/tv?with_genres=10751",
                    "🎌 Anime Series"             to "/discover/tv?with_genres=16&with_origin_country=JP",
                    "💎 Reality TV"               to "/discover/tv?with_genres=10764",
                    "🗣️ Talk Shows"              to "/discover/tv?with_genres=10767",
                    "🌍 International"            to "/discover/tv?with_original_language=fr",
                    "🔥 Hot This Week"            to "/trending/tv/week",
                    "⚡ TOP 10 Today"             to "/trending/tv/day?page=2",
                    "🏆 Award Winners"            to "/discover/tv?sort_by=vote_average.desc&vote_count.gte=500",
                    "🌏 Korean Dramas"            to "/discover/tv?with_origin_country=KR",
                    "🎭 Spanish Series"           to "/discover/tv?with_origin_country=ES",
                    "🌍 African Shows"            to "/discover/tv?with_origin_country=NG",
                    "🎬 Epic Collections"         to "/tv/top_rated?page=2",
                    "⏳ Binge-Worthy"             to "/discover/tv?sort_by=popularity.desc&vote_count.gte=200"
                )

                val genreResults = genreRequests.map { (title, path) ->
                    async {
                        try {
                            val response = tmdbApi.fetch(path)
                            HomeSection(title, response.results ?: emptyList())
                        } catch (e: Exception) { HomeSection(title, emptyList()) }
                    }
                }.awaitAll()

                val combined = (fastResults + genreResults).filter { it.items.isNotEmpty() }
                _tvData.value = combined
                if (combined.isNotEmpty()) {
                    tvLoaded = true
                }

            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error fetching TV Shows", e)
            }
        }
    }

    fun prefetchMedia(id: Int, mediaType: String) {
        viewModelScope.launch {
            try {
                // By calling this, OkHttpClient will automatically cache the response.
                // When the user actually navigates to the DetailScreen, the network request
                // will instantly return from the local HTTP cache, creating a zero-latency experience.
                tmdbApi.fetchDetail("/$mediaType/$id")
            } catch (e: Exception) {
                // Ignore prefetch errors
            }
        }
    }
}
