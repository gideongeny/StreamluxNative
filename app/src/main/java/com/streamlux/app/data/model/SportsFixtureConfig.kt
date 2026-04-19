package com.streamlux.app.data.model

data class SportsFixtureConfig(
    val id: String,
    val leagueId: String,
    val leagueName: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeTeamLogo: String?,
    val awayTeamLogo: String?,
    val status: String,
    val isLive: Boolean,
    val homeScore: Int,
    val awayScore: Int,
    val minute: String,
    val venue: String,
    val kickoffTimeFormatted: String
)
