package com.streamlux.app.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface VylaApi {
    @GET("api/downloads/movie/{tmdbId}")
    suspend fun getMovieDownloads(
        @Path("tmdbId") tmdbId: String
    ): Response<ResponseBody>

    @GET("api/downloads/tv/{tmdbId}/{season}/{episode}")
    suspend fun getTvDownloads(
        @Path("tmdbId") tmdbId: String,
        @Path("season") season: Int,
        @Path("episode") episode: Int
    ): Response<ResponseBody>
}
