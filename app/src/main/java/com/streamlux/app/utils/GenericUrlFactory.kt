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
            "SERVER_A", "SERVER_B", "SERVER_C" -> { 
                // Using Videasy (The exact FMovies+ engine you requested)
                val params = "?color=ff6b00&nextEpisode=true&episodeSelector=true&autoplayNextEpisode=true"
                if (mediaType == "tv" && season > 0) {
                    "https://player.videasy.net/tv/$id/$season/$episode$params"
                } else {
                    "https://player.videasy.net/movie/$id$params"
                }
            }
            "YOUTUBE" -> "https://www.youtube.com/embed/$id?autoplay=1&mute=0&rel=0"
            else -> id
        }
    }
}