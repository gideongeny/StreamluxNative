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
        val isTv = mediaType == "tv" && season > 0
        val s = if (season > 0) season else 1
        val e = if (episode > 0) episode else 1

        return when (providerKey) {
            "VIDKING" -> {
                if (isTv) "https://vidking.net/embed/tv/$id/$s/$e?autoPlay=true"
                else "https://vidking.net/embed/movie/$id?autoPlay=true"
            }
            "VIDEASY" -> {
                val params = "?color=ff6b00&nextEpisode=true&episodeSelector=true&autoplayNextEpisode=true"
                if (isTv) "https://player.videasy.net/tv/$id/$s/$e$params"
                else "https://player.videasy.net/movie/$id$params"
            }
            "VIDSRC_ME" -> {
                if (isTv) "https://vidsrc.me/embed/tv?tmdb=$id&season=$s&episode=$e"
                else "https://vidsrc.me/embed/movie?tmdb=$id"
            }
            "VIDSRC_TO" -> {
                if (isTv) "https://vidsrc.to/embed/tv/$id/$s/$e"
                else "https://vidsrc.to/embed/movie/$id"
            }
            "VIDNEST" -> {
                if (isTv) "https://vidnest.fun/tv/$id/$s/$e?servericon=show&bottomcaption=true"
                else "https://vidnest.fun/movie/$id?servericon=show&bottomcaption=true"
            }
            "TOUSTREAM" -> {
                if (isTv) "https://toustream.xyz/embed/tv/$id/$s/$e"
                else "https://toustream.xyz/embed/movie/$id"
            }
            "ANIMEHUB" -> {
                if (isTv) "https://dropfile.cc/player/tv/$id/$s/$e?audio=sub&lang=en"
                else "https://dropfile.cc/player/movie/$id"
            }
            "FSONIC" -> {
                if (isTv) "https://player.embed-api.stream/?id=$id&s=$s&e=$e"
                else "https://player.embed-api.stream/?id=$id"
            }
            "MIRURO" -> {
                if (isTv) "https://indraembed.netlify.app/tv/$id/$s/$e"
                else "https://indraembed.netlify.app/movie/$id"
            }
            "MEOWTV" -> {
                if (isTv) "https://meowtv.vercel.app/watch/$id?season=$s&ep=$e"
                else "https://meowtv.vercel.app/watch/$id"
            }
            "VIDLINK" -> {
                if (isTv) "https://vidlink.pro/embed/tv/$id/$s/$e?autoPlay=true"
                else "https://vidlink.pro/embed/movie/$id?autoPlay=true"
            }
            "VIDFAST" -> {
                if (isTv) "https://vidfast.pro/tv/$id/$s/$e?autoPlay=true&autoNext=true"
                else "https://vidfast.pro/movie/$id?autoPlay=true"
            }
            "AUTOEMBED" -> {
                if (isTv) "https://autoembed.cc/tv/tmdb/$id-$s-$e"
                else "https://autoembed.cc/movie/tmdb/$id"
            }
            "MOVIEAPI" -> {
                if (isTv) "https://moviesapi.club/tv/$id-$s-$e"
                else "https://moviesapi.club/movie/$id"
            }
            "SMASHYSTREAM" -> {
                if (isTv) "https://embed.smashystream.com/playerjs.php?tmdb=$id&season=$s&episode=$e"
                else "https://embed.smashystream.com/playerjsMovie.php?tmdb=$id"
            }
            "SUPEREMBED" -> {
                if (isTv) "https://multiembed.mov/directstream.php?video_id=$id&tmdb=1&s=$s&e=$e"
                else "https://multiembed.mov/directstream.php?video_id=$id&tmdb=1"
            }
            "YOUTUBE" -> "https://www.youtube.com/embed/$id?autoplay=1&mute=0&rel=0"
            else -> id
        }
    }
}
