package com.streamlux.app.data.api

import com.streamlux.app.data.model.SportsFixture
import com.streamlux.app.data.model.SportsHighlight
import com.streamlux.app.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SportsService @Inject constructor(
    private val client: OkHttpClient,
    private val streamLuxApi: StreamLuxApi
) {
    private val gatewayUrls = listOf(
        "https://streamlux.vercel.app/api/sports",
        "${Constants.API_GATEWAY_BASE}sports",
        "${Constants.TMDB_BACKEND_BASE}/api/sports"
    )

    private val streamedBadgeBase = "https://streamed.pk/api/images/badge"

    suspend fun getLiveMatches(): List<SportsFixture> = withContext(Dispatchers.IO) {
        try {
            coroutineScope {
                val gatewayTask = async { fetchFromGateway("live") }
                val retrofitTask = async { fetchFromRetrofit(true) }
                val espnTask = async { fetchESPNFixtures() }
                val streamedTask = async { fetchStreamedPkLive() }
                val watchfootyTask = async { fetchWatchFootyFixtures() }

                val combined = mergeFixtures(
                    listOf(
                        watchfootyTask.await(),
                        streamedTask.await(),
                        espnTask.await(),
                        gatewayTask.await(),
                        retrofitTask.await()
                    )
                )

                if (combined.isEmpty()) getStaticFixtures() else combined.filter { it.isLive || it.status == "live" }.ifEmpty { combined }
            }
        } catch (e: Exception) {
            fetchESPNFixtures().ifEmpty { getStaticFixtures() }
        }
    }

    suspend fun getUpcomingMatches(): List<SportsFixture> = withContext(Dispatchers.IO) {
        try {
            coroutineScope {
                val gatewayTask = async { fetchFromGateway("upcoming") }
                val retrofitTask = async { fetchFromRetrofit(false) }
                val espnTask = async { fetchESPNFixtures() }
                val watchfootyTask = async { fetchWatchFootyFixtures() }

                val combined = mergeFixtures(
                    listOf(
                        watchfootyTask.await(),
                        espnTask.await(),
                        gatewayTask.await(),
                        retrofitTask.await()
                    )
                )

                combined.filter { !it.isLive && it.status != "live" && it.status != "finished" }
            }
        } catch (e: Exception) {
            fetchESPNFixtures().filter { !it.isLive }
        }
    }

    private suspend fun fetchFromRetrofit(live: Boolean): List<SportsFixture> {
        return try {
            val response = if (live) streamLuxApi.getLiveMatches() else streamLuxApi.getUpcomingMatches()
            response.data
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchFromGateway(kind: String): List<SportsFixture> {
        for (base in gatewayUrls) {
            try {
                val request = Request.Builder()
                    .url("$base/$kind?t=${System.currentTimeMillis()}")
                    .header("User-Agent", "StreamLuxNative/2.3.0")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val parsed = parseFixtures(response.body?.string() ?: "")
                        if (parsed.isNotEmpty()) return parsed
                    }
                }
            } catch (_: Exception) {
                // try next base URL
            }
        }
        return emptyList()
    }

    private fun mergeFixtures(lists: List<List<SportsFixture>>): List<SportsFixture> {
        val map = linkedMapOf<String, SportsFixture>()
        fun key(f: SportsFixture) = "${f.homeTeam}-${f.awayTeam}".lowercase()
        fun score(f: SportsFixture): Int {
            var s = 0
            if (!f.homeTeamLogo.isNullOrBlank()) s += 4
            if (!f.awayTeamLogo.isNullOrBlank()) s += 4
            s += f.streamCount * 2
            if (f.provider == "streamed.pk") s += 5
            if (f.provider == "espn") s += 2
            return s
        }
        for (list in lists) {
            for (fixture in list) {
                val k = key(fixture)
                val existing = map[k]
                if (existing == null || score(fixture) > score(existing)) {
                    map[k] = fixture
                }
            }
        }
        return map.values.toList()
    }

    private suspend fun fetchStreamedPkLive(): List<SportsFixture> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://streamed.pk/api/matches/live")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: "[]"
                val arr = when {
                    body.trimStart().startsWith("[") -> org.json.JSONArray(body)
                    else -> JSONObject(body).optJSONArray("data") ?: org.json.JSONArray()
                }
                val fixtures = mutableListOf<SportsFixture>()
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val title = item.optString("title", "")
                    val isVs = title.contains(" vs ", ignoreCase = true)
                    val parts = if (isVs) title.split(" vs ", limit = 2) else listOf(title, "")
                    val teams = item.optJSONObject("teams")
                    val homeObj = teams?.optJSONObject("home")
                    val awayObj = teams?.optJSONObject("away")
                    val homeName = homeObj?.optString("name")?.takeIf { it.isNotBlank() }
                        ?: parts.getOrElse(0) { "Home" }
                    val awayName = awayObj?.optString("name")?.takeIf { it.isNotBlank() }
                        ?: parts.getOrElse(1) { "" }
                    val homeBadge = homeObj?.optString("badge")
                    val awayBadge = awayObj?.optString("badge")
                    val homeLogo = homeBadge?.let { "$streamedBadgeBase/$it.webp" }
                    val awayLogo = awayBadge?.let { "$streamedBadgeBase/$it.webp" }
                    val sourcesArr = item.optJSONArray("sources")
                    val streamSources = mutableListOf<String>()
                    var streamCount = 0
                    if (sourcesArr != null) {
                        streamCount = sourcesArr.length()
                        for (j in 0 until sourcesArr.length()) {
                            val src = sourcesArr.optJSONObject(j)
                            if (src != null) {
                                streamSources.add(src.optString("source", "src$j"))
                            } else {
                                streamSources.add(sourcesArr.optString(j, "src$j"))
                            }
                        }
                    }
                    val category = item.optString("category", "other")
                    fixtures.add(
                        SportsFixture(
                            id = "spk-${item.optString("id", i.toString())}",
                            leagueName = category.replaceFirstChar { it.uppercase() },
                            homeTeam = homeName,
                            awayTeam = awayName.ifBlank { "Live Broadcast" },
                            homeTeamLogo = homeLogo,
                            awayTeamLogo = awayLogo,
                            status = "live",
                            kickoffTime = "",
                            isLive = true,
                            sport = category,
                            streamCount = streamCount,
                            streamSources = streamSources,
                            isCompetition = !isVs,
                            provider = "streamed.pk"
                        )
                    )
                }
                fixtures
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchESPNFixtures(): List<SportsFixture> = coroutineScope {
        val t = System.currentTimeMillis()
        val endpoints = listOf(
            "https://site.api.espn.com/apis/site/v2/sports/soccer/eng.1/scoreboard?t=$t",
            "https://site.api.espn.com/apis/site/v2/sports/soccer/esp.1/scoreboard?t=$t",
            "https://site.api.espn.com/apis/site/v2/sports/soccer/uefa.champions/scoreboard?t=$t",
            "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard?t=$t",
            "https://site.api.espn.com/apis/site/v2/sports/football/nfl/scoreboard?t=$t"
        )

        endpoints.map { url ->
            async {
                try {
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        parseESPNEvents(response.body?.string() ?: "")
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }.flatMap { it.await() }
    }

    private fun parseFixtures(json: String): List<SportsFixture> {
        val fixtures = mutableListOf<SportsFixture>()
        try {
            val root = JSONObject(json)
            val data = when {
                root.has("data") && root.get("data") is org.json.JSONArray -> root.getJSONArray("data")
                root.has("response") -> root.getJSONArray("response")
                root.has("results") -> root.getJSONArray("results")
                else -> return emptyList()
            }

            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                fixtures.add(
                    SportsFixture(
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
                        venue = item.optString("venue", null),
                        sport = item.optString("sport", item.optString("sportsCategory", null)),
                        streamCount = item.optJSONArray("streamSources")?.length()
                            ?: item.optInt("streamCount", 0),
                        isCompetition = item.optBoolean("isCompetition", false),
                        provider = item.optString("provider", "gateway")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

                    fixtures.add(
                        SportsFixture(
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
                            isLive = isLive,
                            sport = leagueName,
                            provider = "espn"
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        return fixtures
    }

    suspend fun getHighlights(): List<SportsHighlight> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("provider", "scorebat")
                put("params", JSONObject())
            }

            val request = Request.Builder()
                .url("${Constants.API_GATEWAY_BASE}external")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val json = response.body?.string() ?: ""
                parseHighlights(json)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseHighlights(json: String): List<SportsHighlight> {
        val highlights = mutableListOf<SportsHighlight>()
        try {
            val root = JSONObject(json)
            val response = when {
                root.has("response") -> root.getJSONArray("response")
                root.has("data") && root.get("data") is org.json.JSONArray -> root.getJSONArray("data")
                else -> return emptyList()
            }
            for (i in 0 until response.length()) {
                val item = response.getJSONObject(i)
                highlights.add(
                    SportsHighlight(
                        id = "sb_$i",
                        title = item.optString("title", ""),
                        thumbnail = item.optString("thumbnail", ""),
                        embedUrl = if (item.has("videos")) item.getJSONArray("videos").getJSONObject(0).optString("embed", null) else null,
                        competition = item.optString("competition", ""),
                        date = item.optString("date", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return highlights
    }

    private fun getStaticFixtures(): List<SportsFixture> {
        return listOf(
            SportsFixture("ev-1", "Premier League", "Liverpool", "Chelsea", null, null, "upcoming", "", isLive = false, venue = "Anfield"),
            SportsFixture("ev-2", "Champions League", "PSG", "Inter Milan", null, null, "upcoming", "", isLive = false, venue = "Parc des Princes")
        )
    }

    private fun wfAsset(path: String): String {
        return if (path.startsWith("http")) path else "https://api.watchfooty.st$path"
    }

    private fun parseWatchFootyMatches(json: String, sport: String): List<SportsFixture> {
        val list = mutableListOf<SportsFixture>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val m = arr.getJSONObject(i)
                val matchId = m.optString("matchId", "")
                if (matchId.isBlank()) continue
                
                val title = m.optString("title", "")
                val status = m.optString("status", "")
                val isLive = status == "in"
                
                val teams = m.optJSONObject("teams")
                val homeObj = teams?.optJSONObject("home")
                val awayObj = teams?.optJSONObject("away")
                
                val homeName = homeObj?.optString("name") ?: title.split(" vs ").firstOrNull() ?: "Home"
                val awayName = awayObj?.optString("name") ?: title.split(" vs ").getOrNull(1) ?: ""
                
                val homeLogo = homeObj?.optString("logoUrl")?.let { wfAsset(it) }
                val awayLogo = awayObj?.optString("logoUrl")?.let { wfAsset(it) }
                
                val scores = m.optJSONObject("scores")
                val homeScore = scores?.optString("home") ?: "0"
                val awayScore = scores?.optString("away") ?: "0"
                
                val currentMinute = m.optString("currentMinute", "")
                val isEvent = m.optBoolean("isEvent", false)
                
                val timestamp = m.optLong("timestamp", 0L)
                val kickoffTime = if (timestamp > 0) {
                    java.time.Instant.ofEpochMilli(timestamp).toString()
                } else {
                    m.optString("date", "")
                }
                
                val league = m.optString("league", sport.replaceFirstChar { it.uppercase() })
                
                val streamsArr = m.optJSONArray("streams")
                val streamSources = mutableListOf<String>()
                if (streamsArr != null) {
                    for (j in 0 until streamsArr.length()) {
                        val stream = streamsArr.optJSONObject(j)
                        val url = stream?.optString("url")
                        if (!url.isNullOrBlank()) {
                            streamSources.add(url)
                        }
                    }
                }
                
                val poster = m.optString("poster", "")
                val coverUrl = if (poster.isNotBlank()) wfAsset(poster) else null
                
                list.add(
                    SportsFixture(
                        id = "wf-$matchId",
                        leagueName = league,
                        homeTeam = homeName,
                        awayTeam = awayName,
                        homeTeamLogo = homeLogo,
                        awayTeamLogo = awayLogo,
                        status = if (isLive) "live" else if (status == "post") "finished" else "upcoming",
                        kickoffTime = kickoffTime,
                        homeScore = homeScore,
                        awayScore = awayScore,
                        minute = if (isLive) currentMinute.ifBlank { "LIVE" } else null,
                        isLive = isLive,
                        venue = "WatchFooty",
                        sport = sport,
                        streamCount = streamSources.size,
                        streamSources = streamSources,
                        isCompetition = isEvent,
                        coverUrl = coverUrl,
                        provider = "watchfooty"
                    )
                )
            }
        } catch (_: Exception) {
            // ignore parsing error for this sport
        }
        return list
    }

    suspend fun fetchWatchFootyFixtures(): List<SportsFixture> = withContext(Dispatchers.IO) {
        val fixtures = mutableListOf<SportsFixture>()
        val sports = listOf(
            "football",
            "basketball",
            "american-football",
            "baseball",
            "hockey",
            "motorsport",
            "fighting",
            "rugby",
            "tennis",
            "cricket"
        )
        
        try {
            coroutineScope {
                val tasks = sports.map { sport ->
                    async {
                        try {
                            val request = Request.Builder()
                                .url("https://api.watchfooty.st/api/v1/matches/$sport")
                                .header("Referer", "https://watchfooty.st/")
                                .header("Origin", "https://watchfooty.st")
                                .build()
                            
                            client.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    parseWatchFootyMatches(response.body?.string() ?: "", sport)
                                } else {
                                    emptyList()
                                }
                            }
                        } catch (_: Exception) {
                            emptyList()
                        }
                    }
                }
                tasks.forEach { task ->
                    fixtures.addAll(task.await())
                }
            }
        } catch (_: Exception) {
        }
        fixtures
    }
}
