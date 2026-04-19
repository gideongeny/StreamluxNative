package com.streamlux.app.data.api

import com.streamlux.app.data.model.SportsFixture
import com.streamlux.app.data.model.SportsHighlight
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class SportsService @Inject constructor(
    private val client: OkHttpClient
) {
    private val GATEWAY_BASE = "https://us-central1-streamlux-67a84.cloudfunctions.net/gateway"

    suspend fun getLiveMatches(): List<SportsFixture> = withContext(Dispatchers.IO) {
        try {
            val gatewayTask = async { fetchGatewaySports("live") }
            val espnTask = async { fetchESPNFixtures() }
            
            val gatewayResults = try { gatewayTask.await() } catch (e: Exception) { emptyList() }
            val espnResults = try { espnTask.await() } catch (e: Exception) { emptyList() }
            
            val combined = (gatewayResults + espnResults).distinctBy { "${it.homeTeam}-${it.awayTeam}".lowercase() }
            if (combined.isEmpty()) getStaticFixtures() else combined
        } catch (e: Exception) {
            getStaticFixtures()
        }
    }

    suspend fun getUpcomingMatches(): List<SportsFixture> = withContext(Dispatchers.IO) {
        try {
            fetchGatewaySports("upcoming")
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchGatewaySports(kind: String): List<SportsFixture> {
        val request = Request.Builder()
            .url("$GATEWAY_BASE/api/sports/$kind?t=${System.currentTimeMillis()}")
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
            .build()
        
        return client.newCall(request).execute().use { response ->
            val json = response.body?.string() ?: ""
            parseFixtures(json)
        }
    }

    private suspend fun fetchESPNFixtures(): List<SportsFixture> = coroutineScope {
        val endpoints = listOf(
            "https://site.api.espn.com/apis/site/v2/sports/soccer/eng.1/scoreboard",
            "https://site.api.espn.com/apis/site/v2/sports/soccer/esp.1/scoreboard", 
            "https://site.api.espn.com/apis/site/v2/sports/soccer/ger.1/scoreboard",
            "https://site.api.espn.com/apis/site/v2/sports/soccer/ita.1/scoreboard",
            "https://site.api.espn.com/apis/site/v2/sports/soccer/fra.1/scoreboard",
            "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard"
        )
        
        val deferreds = endpoints.map { url ->
            async {
                try {
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        parseESPNEvents(response.body?.string() ?: "")
                    }
                } catch (e: Exception) { emptyList<SportsFixture>() }
            }
        }
        deferreds.flatMap { it.await() }
    }

    private fun parseFixtures(json: String): List<SportsFixture> {
        val fixtures = mutableListOf<SportsFixture>()
        try {
            val root = JSONObject(json)
            val data = when {
                root.has("data") -> root.getJSONArray("data")
                root.has("response") -> root.getJSONArray("response")
                root.has("results") -> root.getJSONArray("results")
                else -> return emptyList()
            }
            
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                fixtures.add(SportsFixture(
                    id = item.optString("id", "match_$i"),
                    leagueName = item.optString("leagueName", "Global League"),
                    homeTeam = item.optString("homeTeam", "Home"),
                    awayTeam = item.optString("awayTeam", "Away"),
                    homeTeamLogo = item.optString("homeTeamLogo", null),
                    awayTeamLogo = item.optString("awayTeamLogo", null),
                    status = item.optString("status", "upcoming"),
                    kickoffTime = item.optString("kickoffTimeFormatted", item.optString("kickoffTime", "")),
                    homeScore = item.optString("homeScore", null),
                    awayScore = item.optString("awayScore", null),
                    minute = item.optString("minute", null),
                    isLive = item.optBoolean("isLive", false) || item.optString("status") == "live",
                    venue = item.optString("venue", null)
                ))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return fixtures
    }

    private fun parseESPNEvents(json: String): List<SportsFixture> {
        val fixtures = mutableListOf<SportsFixture>()
        try {
            val root = JSONObject(json)
            if (!root.has("events")) return emptyList()
            val events = root.getJSONArray("events")
            val leagueName = root.optJSONArray("leagues")?.optJSONObject(0)?.optString("name") ?: "Major League"
            
            for (i in 0 until events.length()) {
                val event = events.getJSONObject(i)
                val statusObj = event.getJSONObject("status")
                val statusType = statusObj.getJSONObject("type")
                val statusName = statusType.optString("name")
                val isLive = statusName.contains("LIVE") || statusName.contains("IN_PROGRESS")
                val isFinished = statusName.contains("FINAL") || statusName.contains("FULL") || statusName.contains("POST")
                val computedStatus = if (isLive) "live" else if (isFinished) "finished" else "upcoming"
                
                val competition = event.getJSONArray("competitions").getJSONObject(0)
                val competitors = competition.getJSONArray("competitors")
                
                var home: JSONObject? = null
                var away: JSONObject? = null
                for (j in 0 until competitors.length()) {
                    val comp = competitors.getJSONObject(j)
                    if (comp.optString("homeAway") == "home") home = comp
                    if (comp.optString("homeAway") == "away") away = comp
                }
                
                if (home != null && away != null) {
                    val homeTeam = home.getJSONObject("team")
                    val awayTeam = away.getJSONObject("team")
                    
                    fixtures.add(SportsFixture(
                        id = "espn-${event.optString("id")}",
                        leagueName = leagueName,
                        homeTeam = homeTeam.optString("displayName"),
                        awayTeam = awayTeam.optString("displayName"),
                        homeTeamLogo = homeTeam.optString("logo"),
                        awayTeamLogo = awayTeam.optString("logo"),
                        status = computedStatus,
                        kickoffTime = event.optString("date"),
                        homeScore = home.optString("score", "0"),
                        awayScore = away.optString("score", "0"),
                        minute = statusObj.optString("displayClock", statusType.optString("shortDetail")),
                        isLive = isLive
                    ))
                }
            }
        } catch (e: Exception) {}
        return fixtures
    }

    suspend fun getHighlights(): List<SportsHighlight> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("provider", "scorebat")
                put("params", JSONObject())
            }
            
            val request = Request.Builder()
                .url("$GATEWAY_BASE/api/proxy/external?t=${System.currentTimeMillis()}")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val json = response.body?.string() ?: ""
                parseHighlights(json)
            }
        } catch (e: Exception) { emptyList<SportsHighlight>() }
    }

    private fun parseHighlights(json: String): List<SportsHighlight> {
        val highlights = mutableListOf<SportsHighlight>()
        try {
            val root = JSONObject(json)
            val response = root.getJSONArray("response")
            for (i in 0 until response.length()) {
                val item = response.getJSONObject(i)
                highlights.add(SportsHighlight(
                    id = "sb_$i",
                    title = item.optString("title", ""),
                    thumbnail = item.optString("thumbnail", ""),
                    embedUrl = if (item.has("videos")) item.getJSONArray("videos").getJSONObject(0).optString("embed", null) else null,
                    competition = item.optString("competition", ""),
                    date = item.optString("date", "")
                ))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return highlights
    }

    private fun getStaticFixtures(): List<SportsFixture> {
        return listOf(
            SportsFixture("ev-1", "Premier League", "Liverpool", "Chelsea", "https://media.api-sports.io/football/teams/40.png", "https://media.api-sports.io/football/teams/49.png", "upcoming", "Tomorrow", isLive = false, venue = "Anfield"),
            SportsFixture("ev-2", "Champions League", "PSG", "Inter Milan", "https://media.api-sports.io/football/teams/85.png", "https://media.api-sports.io/football/teams/505.png", "upcoming", "Next Week", isLive = false, venue = "Parc des Princes")
        )
    }
}
