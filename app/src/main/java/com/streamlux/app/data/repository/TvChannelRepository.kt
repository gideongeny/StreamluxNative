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

    private fun getChannelsUrl(): String =
        "https://streamlux-67a84.web.app/live_channels.json?t=${System.currentTimeMillis()}"

    private fun getKenyanChannelsUrl(): String =
        "https://streamlux-67a84.web.app/kenyan_live_tv.json?t=${System.currentTimeMillis()}"

    suspend fun getSportChannels(): List<TVChannel> =
        getLiveChannels()
            .filter { it.category.equals("Sports", ignoreCase = true) }
            .take(24)

    suspend fun getLiveChannels(): List<TVChannel> {
        cachedChannels?.let { return it }

        return try {
            val kenyan = fetchKenyanChannels()
            val globalJson = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val request = Request.Builder().url(getChannelsUrl()).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                    response.body?.string() ?: throw Exception("Empty body")
                }
            }
            val global = parseChannels(globalJson)
            val kenyanIds = kenyan.map { it.id }.toSet()
            val merged = kenyan + global.filter { it.id !in kenyanIds }
            cachedChannels = merged
            merged
        } catch (e: Exception) {
            e.printStackTrace()
            fetchKenyanChannels().ifEmpty { emptyList() }
        }
    }

    private suspend fun fetchKenyanChannels(): List<TVChannel> {
        return try {
            val json = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val request = Request.Builder().url(getKenyanChannelsUrl()).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    response.body?.string()
                }
            } ?: return emptyList()
            parseChannels(json)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseChannels(json: String): List<TVChannel> {
        val channels = mutableListOf<TVChannel>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val youtubeId = when {
                obj.has("youtubeId") && !obj.isNull("youtubeId") -> obj.getString("youtubeId")
                else -> null
            }
            channels.add(
                TVChannel(
                    id = obj.optString("id"),
                    name = obj.optString("name"),
                    type = obj.optString("type", "iframe"),
                    url = obj.optString("url"),
                    youtubeId = youtubeId,
                    category = obj.optString("category", "Entertainment"),
                    logo = if (obj.has("logo") && !obj.isNull("logo")) obj.getString("logo") else null,
                    isExternal = obj.optBoolean("isExternal", false),
                    country = obj.optString("country", "Global"),
                    countryCode = obj.optString("countryCode", null)
                )
            )
        }
        return channels
    }

    suspend fun seedChannelsOnce(staticChannels: List<TVChannel>) {
        // No-op: channels hosted on Firebase Hosting as JSON
    }
}
