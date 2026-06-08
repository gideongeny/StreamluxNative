package com.streamlux.app.data.model

import com.google.gson.annotations.SerializedName

data class CastItem(
    val name: String,
    val character: String?,
    @SerializedName("profile_path") val profilePath: String?
) {
    val fullProfileUrl: String
        get() = profilePath?.let { "https://image.tmdb.org/t/p/w200$it" } ?: ""
}

data class VideoItem(
    val site: String,
    val type: String,
    val key: String
)

data class CreditsResponse(val cast: List<CastItem>)
data class VideosResponse(val results: List<VideoItem>)

data class Season(
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("episode_count") val episodeCount: Int,
    val name: String,
    @SerializedName("poster_path") val posterPath: String?
)

data class Episode(
    @SerializedName("episode_number") val episodeNumber: Int,
    val name: String,
    val overview: String?,
    @SerializedName("still_path") val stillPath: String?,
    @SerializedName("air_date") val airDate: String?
) {
    val fullStillUrl: String
        get() = stillPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: ""
}

data class SeasonDetailResponse(
    val episodes: List<Episode>
)

data class FilmInfo(
    val detail: TmdbItem,
    val credits: List<CastItem>,
    val similar: List<TmdbItem>,
    val trailerKey: String? // First YouTube Trailer
)

data class VylaDownloadLink(
    val url: String,
    val quality: String,
    val size: String?,
    val format: String,
    val server: Int
)
