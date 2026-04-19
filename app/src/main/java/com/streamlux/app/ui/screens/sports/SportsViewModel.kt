package com.streamlux.app.ui.screens.sports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.streamlux.app.data.api.SportsService
import com.streamlux.app.data.model.SportsFixture
import com.streamlux.app.data.model.SportsHighlight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import javax.inject.Inject

@HiltViewModel
class SportsViewModel @Inject constructor(
    private val sportsService: SportsService
) : ViewModel() {

    private val _liveMatches = MutableStateFlow<List<SportsFixture>>(emptyList())
    val liveMatches: StateFlow<List<SportsFixture>> = _liveMatches

    private val _upcomingMatches = MutableStateFlow<List<SportsFixture>>(emptyList())
    val upcomingMatches: StateFlow<List<SportsFixture>> = _upcomingMatches

    private val _finishedMatches = MutableStateFlow<List<SportsFixture>>(emptyList())
    val finishedMatches: StateFlow<List<SportsFixture>> = _finishedMatches

    private val _highlights = MutableStateFlow<List<SportsHighlight>>(emptyList())
    val highlights: StateFlow<List<SportsHighlight>> = _highlights

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var pollJob: kotlinx.coroutines.Job? = null

    init {
        startPolling()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                _isLoading.value = true
                
                val liveTask = async { sportsService.getLiveMatches() }
                val upcomingTask = async { sportsService.getUpcomingMatches() }
                val highlightsTask = async { sportsService.getHighlights() }
                
                val allRaw = liveTask.await()
                _liveMatches.value = allRaw.filter { it.status == "live" || it.isLive }
                _finishedMatches.value = allRaw.filter { it.status == "finished" }
                
                val incomingUpcoming = upcomingTask.await()
                val espnUpcoming = allRaw.filter { it.status == "upcoming" && !it.isLive }
                val allUpcoming = (incomingUpcoming + espnUpcoming).distinctBy { it.id }
                
                val now = java.time.Instant.now().toEpochMilli()
                val matchDurationMillis = 2 * 60 * 60 * 1000L + 30 * 60 * 1000L // 2.5 hours
                
                _upcomingMatches.value = allUpcoming.filter { fixture ->
                    try {
                        if (fixture.kickoffTime.isNotEmpty() && fixture.kickoffTime.contains("T")) {
                            val kickoff = java.time.Instant.parse(fixture.kickoffTime).toEpochMilli()
                            (now - kickoff) < matchDurationMillis
                        } else {
                            true
                        }
                    } catch (e: Exception) {
                        true
                    }
                }
                
                _highlights.value = highlightsTask.await()
                
                _isLoading.value = false
                kotlinx.coroutines.delay(45000) // Poll every 45 secs
            }
        }
    }

    fun loadSportsData() {
        startPolling()
    }

    fun getStreamUrl(fixture: SportsFixture): String {
        // Parity with React's matchSlug logic: home-vs-away
        val slug = "${fixture.homeTeam}-vs-${fixture.awayTeam}"
            .lowercase()
            .replace(" ", "-")
            .replace(Regex("[^a-z0-9-]"), "")
        
        return "https://sportslive.run/matches/$slug"
    }

    fun getHighlightUrl(highlight: SportsHighlight): String? {
        // Extract URL from embed code if possible
        val embed = highlight.embedUrl ?: return null
        val match = Regex("src=\"([^\"]+)\"").find(embed)
        return match?.groupValues?.get(1) ?: embed
    }
}
