package com.streamlux.app.data.repository

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.streamlux.app.data.api.TmdbApi
import com.streamlux.app.data.model.CreditsResponse
import com.streamlux.app.data.model.SeasonDetailResponse
import com.streamlux.app.data.model.TmdbItem
import com.streamlux.app.data.model.TmdbResponse
import com.streamlux.app.data.model.VideosResponse
import com.streamlux.app.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbRepository @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private data class TmdbCredential(val apiKey: String, val bearer: String)

    private val credentials: List<TmdbCredential> = (if (Constants.TMDB_API_KEY.isNotBlank()) {
        listOf(TmdbCredential(Constants.TMDB_API_KEY, ""))
    } else emptyList()) + listOf(
        TmdbCredential("d87dbd2496ba67b311d9012ed55bc3bb", "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJkODdkYmQyNDk2YmE2N2IzMTFkOTAxMmVkNTViYzNiYiIsIm5iZiI6MTc3OTUwMzM2Ni44MjYsInN1YiI6IjZhMTExMTA2NThjZGZhMjFmNGI1YTVjNSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.tkiRuY9vJkjBVvrJZ-3dLdbj-XPr_m2XsJoJ7C1QWwQ"),
        TmdbCredential("be86af046da20f5bc823fe58fc7ff33e", "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJiZTg2YWYwNDZkYTIwZjViYzgyM2ZlNThmYzdmZjMzZSIsIm5iZiI6MTc3OTUwMzcxNC42MTc5OTk4LCJzdWIiOiI2YTExMTI2MjE3YzM2ZjNjMTBhZWI5ZDkiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.FMZGLJpLNhN942QAV4abXjpNPNtMru7uBovIasui9nc"),
        TmdbCredential("7ea638a69773174284507081474e892d", "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI3ZWE2MzhhNjk3NzMxNzQyODQ1MDcwODE0NzRlODkyZCIsIm5iZiI6MTc1NDgyNjU1Mi4zMTcsInN1YiI6IjY4OTg4NzM4NzczZjAxYzIzNDVkMGRlYSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.dIHNX2XlReX3c8917Ug5Sw9QLf_SEkIIcakh4jCsOos")
    )

    private val gatewayBases = listOf(
        "https://streamlux.vercel.app/api/",
        Constants.API_GATEWAY_BASE,
        "${Constants.TMDB_BACKEND_BASE}/api/"
    )

    suspend fun fetch(endpoint: String, query: String? = null): TmdbResponse {
        fetchDirect(endpoint, query)?.let { return it }
        fetchViaGateway(endpoint, query)?.let { return it }
        try {
            val viaRetrofit = tmdbApi.fetch(endpoint, query)
            if (!viaRetrofit.results.isNullOrEmpty()) return viaRetrofit
        } catch (_: Exception) {
        }
        return TmdbResponse(results = emptyList())
    }

    suspend fun fetchDetail(endpoint: String): TmdbItem {
        try {
            return tmdbApi.fetchDetail(endpoint)
        } catch (_: Exception) {
        }
        return fetchDirectDetail(endpoint)
    }

    suspend fun fetchCredits(endpoint: String): CreditsResponse? =
        fetchTyped(endpoint, CreditsResponse::class.java)

    suspend fun fetchVideos(endpoint: String): VideosResponse? =
        fetchTyped(endpoint, VideosResponse::class.java)

    suspend fun fetchSeasonDetail(endpoint: String): SeasonDetailResponse? =
        fetchTyped(endpoint, SeasonDetailResponse::class.java)

    private suspend fun <T> fetchTyped(endpoint: String, clazz: Class<T>): T? {
        fetchDirectJson(endpoint)?.let { body ->
            try {
                return gson.fromJson(body, clazz)
            } catch (_: Exception) {
            }
        }
        fetchViaGatewayJson(endpoint)?.let { body ->
            try {
                return gson.fromJson(body, clazz)
            } catch (_: Exception) {
            }
        }
        return null
    }

    private suspend fun fetchDirectJson(endpoint: String): String? {
        val (path, extraParams) = splitEndpoint(endpoint)
        for (cred in credentials) {
            try {
                val urlBuilder = "${Constants.TMDB_BASE_URL}$path".toHttpUrlOrNull()?.newBuilder()
                    ?: continue
                urlBuilder.addQueryParameter("api_key", cred.apiKey)
                extraParams.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }

                val body = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(
                        Request.Builder()
                            .url(urlBuilder.build())
                            .header("Accept", "application/json")
                            .apply {
                                if (cred.bearer.isNotBlank()) {
                                    header("Authorization", "Bearer ${cred.bearer}")
                                }
                            }
                            .get()
                            .build()
                    ).execute().use { res ->
                        if (!res.isSuccessful) return@use null
                        res.body?.string()
                    }
                } ?: continue
                if (body.isNotBlank()) return body
            } catch (_: Exception) {
            }
        }
        return null
    }

    private suspend fun fetchViaGatewayJson(endpoint: String): String? {
        val (path, extraParams) = splitEndpoint(endpoint)
        for (base in gatewayBases) {
            try {
                val urlBuilder = "${base}tmdb".toHttpUrlOrNull()?.newBuilder() ?: continue
                urlBuilder.addQueryParameter("endpoint", path)
                extraParams.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }

                val body = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(Request.Builder().url(urlBuilder.build()).get().build())
                        .execute().use { res ->
                            if (!res.isSuccessful) return@use null
                            res.body?.string()
                        }
                } ?: continue
                if (body.isNotBlank()) return body
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun splitEndpoint(endpoint: String): Pair<String, Map<String, String>> {
        val raw = if (endpoint.startsWith("/")) endpoint else "/$endpoint"
        val path = raw.substringBefore('?')
        val queryPart = raw.substringAfter('?', "")
        val extra = mutableMapOf<String, String>()
        if (queryPart.isNotEmpty()) {
            queryPart.split('&').forEach { pair ->
                val kv = pair.split('=', limit = 2)
                if (kv.size == 2) extra[kv[0]] = Uri.decode(kv[1])
            }
        }
        return path to extra
    }

    private fun parseResults(body: String): List<TmdbItem> {
        return try {
            val json = JsonParser.parseString(body)
            if (!json.isJsonObject) return emptyList()
            val obj = json.asJsonObject
            if (obj.has("results") && obj.get("results").isJsonArray) {
                gson.fromJson(obj.getAsJsonArray("results"), Array<TmdbItem>::class.java).toList()
            } else if (obj.has("parts") && obj.get("parts").isJsonArray) {
                gson.fromJson(obj.getAsJsonArray("parts"), Array<TmdbItem>::class.java).toList()
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchDirect(endpoint: String, query: String?): TmdbResponse? {
        val (path, extraParams) = splitEndpoint(endpoint)
        for (cred in credentials) {
            try {
                val urlBuilder = "${Constants.TMDB_BASE_URL}$path".toHttpUrlOrNull()?.newBuilder()
                    ?: continue
                urlBuilder.addQueryParameter("api_key", cred.apiKey)
                if (!query.isNullOrBlank()) urlBuilder.addQueryParameter("query", query)
                extraParams.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }

                val request = Request.Builder()
                    .url(urlBuilder.build())
                    .header("Accept", "application/json")
                    .apply {
                        if (cred.bearer.isNotBlank()) header("Authorization", "Bearer ${cred.bearer}")
                    }
                    .get()
                    .build()

                val body = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute().use { res ->
                        if (!res.isSuccessful) return@use null
                        res.body?.string()
                    }
                } ?: continue

                val items = parseResults(body)
                if (items.isNotEmpty()) return TmdbResponse(results = items)
            } catch (_: Exception) {
            }
        }
        return null
    }

    private suspend fun fetchViaGateway(endpoint: String, query: String?): TmdbResponse? {
        val (path, extraParams) = splitEndpoint(endpoint)
        for (base in gatewayBases) {
            try {
                val urlBuilder = "${base}tmdb".toHttpUrlOrNull()?.newBuilder() ?: continue
                urlBuilder.addQueryParameter("endpoint", path)
                if (!query.isNullOrBlank()) urlBuilder.addQueryParameter("query", query)
                extraParams.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }

                val request = Request.Builder().url(urlBuilder.build()).get().build()
                val body = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute().use { res ->
                        if (!res.isSuccessful) return@use null
                        res.body?.string()
                    }
                } ?: continue

                val items = parseResults(body)
                if (items.isNotEmpty()) return TmdbResponse(results = items)
            } catch (_: Exception) {
            }
        }
        return null
    }

    private suspend fun fetchDirectDetail(endpoint: String): TmdbItem {
        val body = fetchDirectJson(endpoint)
            ?: throw IllegalStateException("TMDB detail unavailable")
        return gson.fromJson(body, TmdbItem::class.java)
    }
}
