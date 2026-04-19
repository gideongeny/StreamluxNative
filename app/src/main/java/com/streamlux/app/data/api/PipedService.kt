package com.streamlux.app.data.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipedService @Inject constructor(
    private val client: OkHttpClient
) {
    private val instances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://api.piped.ot.ax",
        "https://pipedapi.drgns.space",
        "https://api.piped.privacy.com.de",
        "https://piped-api.lunar.icu"
    )

    suspend fun getStreamUrl(videoId: String): String? {
        // Try up to 3 instances
        for (i in 0 until 3) {
            val base = instances[i % instances.size]
            try {
                val request = Request.Builder()
                    .url("$base/streams/$videoId")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val json = response.body?.string() ?: return@use
                    val root = JSONObject(json)
                    
                    val audioStreams = root.optJSONArray("audioStreams")
                    if (audioStreams != null && audioStreams.length() > 0) {
                        // Find M4A or highest bitrate
                        var bestUrl: String? = null
                        var bestBitrate = -1
                        
                        for (j in 0 until audioStreams.length()) {
                            val stream = audioStreams.getJSONObject(j)
                            val bitrate = stream.optInt("bitrate", 0)
                            if (bitrate > bestBitrate) {
                                bestBitrate = bitrate
                                bestUrl = stream.optString("url")
                            }
                        }
                        if (bestUrl != null) return bestUrl
                    }
                }
            } catch (e: Exception) {
                Log.w("PipedService", "Instance $base failed for $videoId: ${e.message}")
            }
        }
        return null
    }
}
