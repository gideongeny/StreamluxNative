package com.streamlux.app.utils

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenericUrlFactory @Inject constructor() {

    fun create(
        id: String,
        season: Int,
        episode: Int,
        mediaType: String,
        providerKey: String
    ): String {
        return when (providerKey) {
            "VIDEASY" -> { 
                // Using Videasy (The exact FMovies+ engine you requested)
                val params = "?color=ff6b00&nextEpisode=true&episodeSelector=true&autoplayNextEpisode=true"
                if (mediaType == "tv" && season > 0) {
                    "https://player.videasy.net/tv/$id/$season/$episode$params"
                } else {
                    "https://player.videasy.net/movie/$id$params"
                }
            }
            "VIDSRC_ME" -> {
                if (mediaType == "tv" && season > 0) {
                    "https://vidsrc.me/embed/tv?tmdb=$id&season=$season&episode=$episode"
                } else {
                    "https://vidsrc.me/embed/movie?tmdb=$id"
                }
            }
            "VIDSRC_TO" -> {
                if (mediaType == "tv" && season > 0) {
                    "https://vidsrc.to/embed/tv/$id/$season/$episode"
                } else {
                    "https://vidsrc.to/embed/movie/$id"
                }
            }
            "SUPEREMBED" -> {
                 if (mediaType == "tv" && season > 0) {
                    "https://multiembed.mov/directstream.php?video_id=$id&tmdb=1&s=$season&e=$episode"
                } else {
                    "https://multiembed.mov/directstream.php?video_id=$id&tmdb=1"
                }
            }
            "YOUTUBE" -> "https://www.youtube.com/embed/$id?autoplay=1&mute=0&rel=0"
            else -> id
        }
    }
}