package com.streamlux.app.data.api

import com.streamlux.app.data.model.TmdbItem
import com.streamlux.app.data.model.TmdbResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApi {
    @GET("tmdb")
    suspend fun fetch(@Query("endpoint") endpoint: String): TmdbResponse

    @GET("tmdb")
    suspend fun fetchDetail(@Query("endpoint") endpoint: String): TmdbItem
    
    @GET("tmdb")
    suspend fun fetchCredits(@Query("endpoint") endpoint: String): com.streamlux.app.data.model.CreditsResponse
    
    @GET("tmdb")
    suspend fun fetchVideos(@Query("endpoint") endpoint: String): com.streamlux.app.data.model.VideosResponse
    
    @GET("tmdb")
    suspend fun fetchSeasonDetail(@Query("endpoint") endpoint: String): com.streamlux.app.data.model.SeasonDetailResponse
}
