package com.streamlux.app.data.model

data class SportsFixture(
    val id: String,
    val leagueName: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeTeamLogo: String?,
    val awayTeamLogo: String?,
    val status: String,
    val kickoffTime: String,
    val homeScore: String? = null,
    val awayScore: String? = null,
    val minute: String? = null,
    val isLive: Boolean = false,
    val venue: String? = null,
    val countdown: String? = null,
    val sport: String? = null,
    val streamCount: Int = 0,
    val streamSources: List<String> = emptyList(),
    /** True for single-event cards (UFC main card, F1 race) — not team vs team */
    val isCompetition: Boolean = false,
    val coverUrl: String? = null,
    val provider: String = "unknown"
) {
    val isVsMatch: Boolean get() = !isCompetition && awayTeam.isNotBlank() && awayTeam != "Away" && awayTeam != "Live Broadcast"
    val displayStreamCount: Int get() = streamCount.coerceAtLeast(if (isLive) 1 else 0)
}

data class SportsHighlight(
    val id: String,
    val title: String,
    val thumbnail: String,
    val embedUrl: String?,
    val competition: String,
    val date: String
)

data class SportsDataResponse(
    val success: Boolean? = null,
    val data: List<SportsFixture> = emptyList()
)
