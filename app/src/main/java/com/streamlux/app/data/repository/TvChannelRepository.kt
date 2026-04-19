package com.streamlux.app.data.repository

import com.streamlux.app.data.model.TVChannel
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvChannelRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private var cachedChannels: List<TVChannel>? = null

    private val CHANNELS_URL =
        "https://streamlux-67a84.web.app/live_channels.json"

    suspend fun getLiveChannels(): List<TVChannel> {
        cachedChannels?.let { return it }

        return try {
            val json = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val request = Request.Builder()
                    .url(CHANNELS_URL)
                    .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                    response.body?.string() ?: throw Exception("Empty body")
                }
            }
            val result = parseChannels(json)
            cachedChannels = result
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseChannels(json: String): List<TVChannel> {
        val channels = mutableListOf<TVChannel>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            channels.add(
                TVChannel(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    type = obj.optString("type", "iframe"),
                    url = obj.optString("url"),
                    category = obj.optString("category", "Entertainment"),
                    logo = if (obj.has("logo") && !obj.isNull("logo")) obj.getString("logo") else null,
                    isExternal = obj.optBoolean("isExternal", false)
                )
            )
        }
        return channels
    }

    suspend fun seedChannelsOnce(staticChannels: List<TVChannel>) {
        // No-op: channels hosted on Firebase Hosting as JSON
    }
}
