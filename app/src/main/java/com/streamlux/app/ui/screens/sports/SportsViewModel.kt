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
                
                val now = System.currentTimeMillis()
                val matchDurationMillis = 3 * 60 * 60 * 1000L // 3 hours cutoff for stale matches

                val rawLive = liveTask.await()
                _liveMatches.value = rawLive.filter { it.status == "live" || it.isLive }
                _finishedMatches.value = rawLive.filter { it.status == "finished" }
                
                val incomingUpcoming = upcomingTask.await()
                val espnUpcoming = rawLive.filter { it.status == "upcoming" && !it.isLive }
                val allUpcoming = (incomingUpcoming + espnUpcoming).distinctBy { it.id }
                
                _upcomingMatches.value = allUpcoming.filter { fixture ->
                    try {
                        val kickoff = if (fixture.kickoffTime.isNotEmpty()) {
                            if (fixture.kickoffTime.contains("T")) {
                                java.time.Instant.parse(fixture.kickoffTime).toEpochMilli()
                            } else if (fixture.kickoffTime.contains("-")) {
                                // Fallback for simple date strings YYYY-MM-DD
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                sdf.parse(fixture.kickoffTime.split(" ")[0])?.time ?: 0L
                            } else 0L
                        } else 0L

                        if (kickoff > 0) {
                            // If match started more than 3 hours ago, hide it (stale)
                            (now - kickoff) < matchDurationMillis
                        } else {
                            // If no parseable date, only hide if it's "Yesterday"
                            !fixture.kickoffTime.lowercase().contains("yesterday")
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
