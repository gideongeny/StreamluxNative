package com.streamlux.app.utils

import com.streamlux.app.BuildConfig

object Constants {
    // Groq AI API key
    val GROQ_API_KEY = BuildConfig.GROQ_API_KEY

    // Google Web Client ID
    val GOOGLE_WEB_CLIENT_ID = BuildConfig.GOOGLE_WEB_CLIENT_ID

    // Firebase Project
    const val FIREBASE_PROJECT_ID = "streamlux-67a84"
    val FIREBASE_API_KEY = BuildConfig.FIREBASE_API_KEY
    val FIREBASE_MESSAGING_SENDER_ID = BuildConfig.FIREBASE_MESSAGING_SENDER_ID
    val FIREBASE_APP_ID = BuildConfig.FIREBASE_APP_ID

    // Media Discovery & Metadata
    val TMDB_API_KEY = BuildConfig.TMDB_API_KEY
    const val TMDB_BACKEND_BASE = "https://us-central1-streamlux-67a84.cloudfunctions.net/gateway"
    const val TMDB_BASE_URL = "https://api.themoviedb.org/3"
    const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p"

    val OMD_API_KEY = BuildConfig.OMD_API_KEY
    val FANART_API_KEY = BuildConfig.FANART_API_KEY
    val TRAKT_CLIENT_ID = BuildConfig.TRAKT_CLIENT_ID
    val TASTEDIVE_API_KEY = BuildConfig.TASTEDIVE_API_KEY
    val WATCHMODE_API_KEY = BuildConfig.WATCHMODE_API_KEY

    // Sports Intelligence (APIsport, Scorebat)
    val APISPORTS_KEY = BuildConfig.APISPORTS_KEY
    val SCOREBAT_TOKEN = BuildConfig.SCOREBAT_TOKEN

    // YouTube API keys (rotation pool)
    val YOUTUBE_API_KEY_1 = BuildConfig.YOUTUBE_API_KEY_1
    val YOUTUBE_API_KEY_2 = BuildConfig.YOUTUBE_API_KEY_2
    val YOUTUBE_API_KEY_3 = BuildConfig.YOUTUBE_API_KEY_3
    val YOUTUBE_API_KEY_4 = BuildConfig.YOUTUBE_API_KEY_4
    
    // SerpApi for Short Videos
    const val SERP_API_KEY = "YOUR_SERPAPI_KEY" // Placeholder: User should update in local.properties


    // Video Embed Sources (exact formats from React app download.ts / resolver.ts)
    const val VIDSRC_XYZ  = "https://vidsrc.xyz/embed"
    const val VIDSRC_ME   = "https://vidsrc.me/embed"
    const val VIDSRC_TO   = "https://vidsrc.to/embed"
    const val VIDSRC_PRO  = "https://vidsrc.pro/embed"
    const val AUTOEMBED   = "https://player.autoembed.cc/embed"
    const val VIDLINK     = "https://vidlink.pro"
    const val TWOEMBED_TO = "https://www.2embed.to/embed/tmdb"
    const val MULTIEMBED  = "https://multiembed.mov"
    const val APIMDB      = "https://v2.apimdb.net/e"
    const val SMASHY      = "https://embed.smashystream.com/playere.php"
    const val SMASHY2     = "https://smashystream.xyz/player.php"
    const val VIDPLAY     = "https://vidplay.online/e"
    const val FZMOVIES_EMBED = "https://fzmovies.cms/embed"
    const val TWOEMBED_CC = "https://2embed.cc/embed"
}
