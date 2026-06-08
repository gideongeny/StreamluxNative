package com.streamlux.app.data.api

import com.streamlux.app.data.model.ShortVideoResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SerpApiService {
    @GET("proxy/external")
    suspend fun getShortVideos(
        @Query("q") query: String,
        @Query("provider") provider: String = "serpapi"
    ): ShortVideoResponse
}
