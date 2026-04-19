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

    private val _ticker = MutableStateFlow(System.currentTimeMillis())

    init {
        startPolling()
        viewModelScope.launch {
            while(true) {
                _ticker.value = System.currentTimeMillis()
                kotlinx.coroutines.delay(1000)
            }
        }
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
                val matchDurationMillis = 3 * 60 * 60 * 1000L

                val rawLive = liveTask.await()
                val incomingUpcoming = upcomingTask.await()
                
                val allMatches = (rawLive + incomingUpcoming).distinctBy { it.id }
                
                val currentLive = mutableListOf<SportsFixture>()
                val currentUpcoming = mutableListOf<SportsFixture>()
                val currentFinished = mutableListOf<SportsFixture>()

                allMatches.forEach { fixture ->
                    val kickoff = try {
                        if (fixture.kickoffTime.contains("T")) {
                            java.time.Instant.parse(fixture.kickoffTime).toEpochMilli()
                        } else if (fixture.kickoffTime.contains("-")) {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                            sdf.parse(fixture.kickoffTime.split(" ")[0])?.time ?: 0L
                        } else 0L
                    } catch (e: Exception) { 0L }

                    val isExpired = kickoff > 0 && (now - kickoff) >= matchDurationMillis
                    val isLiveNow = (fixture.status == "live" || fixture.isLive) && !isExpired
                    val hasStarted = kickoff > 0 && now >= kickoff && !isExpired

                    if (isExpired) {
                        // Gone forever after 3 hours
                    } else if (isLiveNow || hasStarted) {
                        currentLive.add(fixture.copy(status = "live", isLive = true))
                    } else if (fixture.status == "finished") {
                        currentFinished.add(fixture)
                    } else {
                        val countdownStr = if (kickoff > now) formatCountdown(kickoff - now) else null
                        currentUpcoming.add(fixture.copy(countdown = countdownStr))
                    }
                }

                _liveMatches.value = currentLive
                _upcomingMatches.value = currentUpcoming
                _finishedMatches.value = currentFinished
                _highlights.value = highlightsTask.await()
                
                _isLoading.value = false
                kotlinx.coroutines.delay(45000)
            }
        }
    }

    private fun formatCountdown(diff: Long): String {
        val seconds = (diff / 1000) % 60
        val minutes = (diff / (1000 * 60)) % 60
        val hours = (diff / (1000 * 60 * 60))
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
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
