package com.streamlux.app.data.api

import com.streamlux.app.data.model.MusicTrack
import com.streamlux.app.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicService @Inject constructor(
    private val client: OkHttpClient
) {
    private val GATEWAY_BASE = "https://us-central1-streamlux-67a84.cloudfunctions.net/gateway"

    suspend fun getTrending(): List<MusicTrack> {
        return try {
            val request = Request.Builder()
                .url("https://saavn.dev/api/modules?lang=hindi,english&page=1")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                val json = response.body?.string() ?: ""
                val tracks = parseSaavnSongs(json)
                if (tracks.isEmpty()) fetchFromSaavnDirect("api/search/songs?query=trending+music&limit=40") else tracks
            }
        } catch (e: Exception) {
            fetchFromSaavnDirect("api/search/songs?query=popular+songs&limit=40")
        }
    }

    suspend fun search(query: String): List<MusicTrack> {
        return try {
            val request = Request.Builder()
                .url("$GATEWAY_BASE/music/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Search failed")
                val json = response.body?.string() ?: ""
                parseSaavnSongs(json)
            }
        } catch (e: Exception) {
            fetchFromSaavnDirect("api/search/songs?query=${java.net.URLEncoder.encode(query, "UTF-8")}&limit=30")
        }
    }

    private fun fetchFromSaavnDirect(endpoint: String): List<MusicTrack> {
        return try {
            val request = Request.Builder()
                .url("https://saavn.dev/$endpoint")
                .build()

            client.newCall(request).execute().use { response ->
                val json = response.body?.string() ?: ""
                parseSaavnSongs(json)
            }
        } catch (e: Exception) {
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
                    if (songs is JSONArray) songs else if (songs is JSONObject && songs.has("results")) songs.getJSONArray("results") else JSONArray()
                }
                data is JSONObject && data.has("results") -> data.getJSONArray("results")
                else -> JSONArray()
            }

            for (i in 0 until songsArray.length()) {
                val item = songsArray.getJSONObject(i)
                tracks.add(mapToTrack(item))
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
