package com.streamlux.app.utils

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object LiveStreamUrl {
    /** Must match web `youtubeLiveTV.ts` origin for embed allowlists */
    const val YOUTUBE_EMBED_ORIGIN = "https://streamlux-67a84.web.app"

    private val youtubeIdPatterns = listOf(
        Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/|youtube-nocookie\\.com/embed/|youtube\\.com/live/)([\\w-]{11})", RegexOption.IGNORE_CASE),
        Regex("youtube\\.com/watch\\?[^#]*[&?]v=([\\w-]{11})", RegexOption.IGNORE_CASE)
    )

    fun youtubeVideoId(url: String): String? {
        if (url.isBlank()) return null
        val trimmed = url.trim()
        if (Regex("^[\\w-]{11}$").matches(trimmed)) return trimmed
        for (pattern in youtubeIdPatterns) {
            pattern.find(trimmed)?.groupValues?.getOrNull(1)?.let { return it }
        }
        return null
    }

    /** Nocookie embed tuned for autoplay in WebView (mute required) */
    fun youtubeLiveEmbedUrl(videoId: String): String {
        val params = linkedMapOf(
            "autoplay" to "1",
            "mute" to "1",
            "playsinline" to "1",
            "controls" to "1",
            "rel" to "0",
            "modestbranding" to "1",
            "enablejsapi" to "1",
            "iv_load_policy" to "3",
            "fs" to "1",
            "origin" to YOUTUBE_EMBED_ORIGIN
        )
        val query = params.entries.joinToString("&") { (k, v) ->
            "$k=${URLEncoder.encode(v, StandardCharsets.UTF_8.name())}"
        }
        return "https://www.youtube-nocookie.com/embed/$videoId?$query"
    }

    fun youtubeWatchUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"

    /** HTML wrapper so WebView base URL matches allowed embed origin */
    fun youtubeLiveEmbedHtml(videoId: String): String {
        val src = youtubeLiveEmbedUrl(videoId)
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0"/>
              <style>
                html, body { margin:0; padding:0; background:#000; height:100%; overflow:hidden; }
                iframe { position:fixed; inset:0; width:100%; height:100%; border:0; }
              </style>
            </head>
            <body>
              <iframe
                src="$src"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share; fullscreen"
                allowfullscreen
                referrerpolicy="strict-origin-when-cross-origin"
              ></iframe>
            </body>
            </html>
        """.trimIndent()
    }
}
