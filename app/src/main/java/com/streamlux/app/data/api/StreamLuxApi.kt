package com.streamlux.app.data.api

import com.streamlux.app.data.model.SportsDataResponse
import retrofit2.http.GET
import retrofit2.http.Query
import com.google.gson.JsonElement

interface StreamLuxApi {
    @GET("sports/live")
    suspend fun getLiveMatches(): SportsDataResponse

    @GET("sports/upcoming")
    suspend fun getUpcomingMatches(): SportsDataResponse

    @GET("music/trending")
    suspend fun getTrendingMusic(): JsonElement

    @GET("music/search")
    suspend fun searchMusic(@Query("q") query: String): JsonElement
}
