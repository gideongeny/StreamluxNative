package com.streamlux.app.data.model

data class MusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val duration: String? = null,
    val thumbnail: String,
    val streamUrl: String?,
    val source: String
)
