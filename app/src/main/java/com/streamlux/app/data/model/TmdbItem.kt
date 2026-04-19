package com.streamlux.app.data.model

import com.google.gson.annotations.SerializedName

data class TmdbResponse(
    val results: List<TmdbItem>
)

data class TmdbItem(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("media_type") val mediaType: String? = null,
    val overview: String? = null,
    @SerializedName("vote_average") val voteAverage: Double? = null,
    val runtime: Int? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    val seasons: List<Season>? = null
) {
    val displayTitle: String get() = title ?: name ?: "Unknown"
    val fullPosterUrl: String get() = "https://image.tmdb.org/t/p/w500$posterPath"
}
