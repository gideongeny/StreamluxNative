package com.streamlux.app.data.model

data class SportsFixture(
    val id: String,
    val leagueName: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeTeamLogo: String?,
    val awayTeamLogo: String?,
    val status: String, // "live", "upcoming", "finished"
    val kickoffTime: String,
    val homeScore: String? = null,
    val awayScore: String? = null,
    val minute: String? = null,
    val isLive: Boolean = false,
    val venue: String? = null
)

data class SportsHighlight(
    val id: String,
    val title: String,
    val thumbnail: String,
    val embedUrl: String?,
    val competition: String,
    val date: String
)

data class SportsDataResponse(
    val data: List<SportsFixture>
)
