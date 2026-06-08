package com.streamlux.app.data.api

import com.streamlux.app.data.model.MusicTrack
import com.streamlux.app.utils.Constants
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicService @Inject constructor(
    private val client: OkHttpClient
) {
    private val pipedInstances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://api.piped.ot.ax",
        "https://pipedapi.drgns.space",
        "https://piped-api.lunar.icu"
    )

    suspend fun getTrending(): List<MusicTrack> = withContext(Dispatchers.IO) {
        val query = "Global Top Hits 2025 Music Playlist official"
        searchPiped(query, "music_songs")
            .ifEmpty { searchInnerTube(query) }
            .ifEmpty { fetchFromSaavnDirect("api/search/songs?query=popular+songs&limit=40") }
    }

    suspend fun search(query: String): List<MusicTrack> = withContext(Dispatchers.IO) {
        val q = if (query.contains("playlist", ignoreCase = true)) query else "$query music official audio"
        searchPiped(q, "music_songs")
            .ifEmpty { searchInnerTube(q) }
            .ifEmpty { fetchFromSaavnDirect("api/search/songs?query=${java.net.URLEncoder.encode(query, "UTF-8")}&limit=30") }
    }

    private fun searchPiped(query: String, filter: String): List<MusicTrack> {
        for (base in pipedInstances) {
            try {
                val url = "$base/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&filter=$filter"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val root = JSONObject(response.body?.string() ?: "{}")
                    val items = root.optJSONArray("items") ?: return@use
                    val tracks = mutableListOf<MusicTrack>()
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        if (item.optString("type") != "stream") continue
                        val videoUrl = item.optString("url", "")
                        val id = videoUrl.substringAfter("v=").substringBefore("&")
                        if (id.isBlank()) continue
                        tracks.add(
                            MusicTrack(
                                id = id,
                                title = item.optString("title", "Unknown"),
                                artist = item.optString("uploaderName", "Unknown Artist"),
                                thumbnail = item.optString("thumbnail", ""),
                                source = "youtube"
                            )
                        )
                    }
                    if (tracks.isNotEmpty()) return tracks
                }
            } catch (_: Exception) {
                // try next instance
            }
        }
        return emptyList()
    }

    private fun searchInnerTube(query: String): List<MusicTrack> {
        val body = JSONObject().apply {
            put("endpoint", "/search")
            put("query", query)
            put("client", "WEB_REMIX")
        }
        val postBody = body.toString().toRequestBody("application/json".toMediaType())
        val urls = listOf(
            "${Constants.API_GATEWAY_BASE}music/innertube",
            "https://streamlux.vercel.app/api/music/innertube"
        )
        for (url in urls) {
            try {
                val request = Request.Builder().url(url).post(postBody).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val tracks = parseInnerTubeSongs(response.body?.string() ?: "")
                        if (tracks.isNotEmpty()) return tracks
                    }
                }
            } catch (_: Exception) {
                // try next gateway
            }
        }
        return emptyList()
    }

    private fun parseInnerTubeSongs(json: String): List<MusicTrack> {
        val tracks = mutableListOf<MusicTrack>()
        try {
            val root = JSONObject(json)
            val found = mutableListOf<JSONObject>()
            collectInnerTubeItems(root, found)
            for (item in found) {
                val videoId = item.optJSONObject("playlistItemData")?.optString("videoId")
                    ?: item.optJSONObject("overlay")
                        ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                        ?.optJSONObject("content")
                        ?.optJSONObject("musicPlayButtonRenderer")
                        ?.optJSONObject("playNavigationEndpoint")
                        ?.optJSONObject("watchEndpoint")
                        ?.optString("videoId")
                if (videoId.isNullOrBlank()) continue
                val columns = item.optJSONArray("flexColumns") ?: continue
                val title = columns.optJSONObject(0)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")
                    ?.optJSONArray("runs")
                    ?.optJSONObject(0)
                    ?.optString("text") ?: "Unknown"
                val artistRuns = columns.optJSONObject(1)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")
                    ?.optJSONArray("runs")
                val artist = buildString {
                    if (artistRuns != null) {
                        for (i in 0 until artistRuns.length()) {
                            val t = artistRuns.optJSONObject(i)?.optString("text") ?: continue
                            if (t != " • ") append(t)
                        }
                    }
                }.ifBlank { "Unknown Artist" }
                tracks.add(
                    MusicTrack(
                        id = videoId,
                        title = title,
                        artist = artist,
                        thumbnail = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                        source = "youtube"
                    )
                )
            }
        } catch (_: Exception) {
        }
        return tracks.distinctBy { it.id }
    }

    private fun collectInnerTubeItems(node: Any?, out: MutableList<JSONObject>) {
        when (node) {
            is JSONObject -> {
                if (node.has("musicResponsiveListItemRenderer")) {
                    out.add(node.getJSONObject("musicResponsiveListItemRenderer"))
                }
                node.keys().forEach { key -> collectInnerTubeItems(node.get(key), out) }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) collectInnerTubeItems(node.get(i), out)
            }
        }
    }

    private fun fetchFromSaavnDirect(endpoint: String): List<MusicTrack> {
        return try {
            val request = Request.Builder()
                .url("https://saavn.dev/$endpoint")
                .build()
            client.newCall(request).execute().use { response ->
                parseSaavnSongs(response.body?.string() ?: "")
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseSaavnSongs(json: String): List<MusicTrack> {
        val tracks = mutableListOf<MusicTrack>()
        try {
            val root = JSONObject(json)
            val data = if (root.has("data")) root.get("data") else root

            val songsArray = when {
                data is JSONArray -> data
                data is JSONObject && data.has("trending") -> {
                    val trending = data.getJSONObject("trending")
                    if (trending.has("songs")) trending.getJSONArray("songs") else JSONArray()
                }
                data is JSONObject && data.has("songs") -> {
                    val songs = data.get("songs")
                    when (songs) {
                        is JSONArray -> songs
                        is JSONObject -> songs.optJSONArray("results") ?: JSONArray()
                        else -> JSONArray()
                    }
                }
                data is JSONObject && data.has("results") -> data.getJSONArray("results")
                else -> JSONArray()
            }

            for (i in 0 until songsArray.length()) {
                tracks.add(mapToTrack(songsArray.getJSONObject(i)))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return tracks
    }

    private fun mapToTrack(item: JSONObject): MusicTrack {
        val id = item.optString("id", Math.random().toString())
        val name = item.optString("name", item.optString("title", "Unknown"))

        val artist = if (item.has("artists")) {
            val artists = item.getJSONObject("artists")
            if (artists.has("primary")) {
                val primaryArray = artists.getJSONArray("primary")
                val names = mutableListOf<String>()
                for (j in 0 until primaryArray.length()) {
                    names.add(primaryArray.getJSONObject(j).optString("name"))
                }
                names.joinToString(", ")
            } else item.optString("primaryArtists", "Various Artists")
        } else item.optString("primaryArtists", item.optString("artist", "Various Artists"))

        val imageArray = item.optJSONArray("image")
        val thumbnail = if (imageArray != null && imageArray.length() > 0) {
            val idx = if (imageArray.length() >= 3) 2 else imageArray.length() - 1
            imageArray.getJSONObject(idx).optString("link", imageArray.getJSONObject(idx).optString("url", ""))
        } else {
            item.optString("thumbnail", "")
        }

        val downloadUrlArray = item.optJSONArray("downloadUrl")
        val streamUrl = if (downloadUrlArray != null && downloadUrlArray.length() > 0) {
            val idx = if (downloadUrlArray.length() >= 5) 4 else downloadUrlArray.length() - 1
            downloadUrlArray.getJSONObject(idx).optString("link", downloadUrlArray.getJSONObject(idx).optString("url", ""))
        } else {
            item.optString("url", null)
        }

        return MusicTrack(
            id = id,
            title = name,
            artist = artist,
            thumbnail = thumbnail,
            album = if (item.has("album") && item.get("album") is JSONObject) item.getJSONObject("album").optString("name") else item.optString("album", null),
            duration = item.optString("duration", null),
            streamUrl = streamUrl,
            source = "saavn"
        )
    }
}
