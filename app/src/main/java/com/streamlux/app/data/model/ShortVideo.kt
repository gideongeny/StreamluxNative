package com.streamlux.app.data.model

import com.google.gson.annotations.SerializedName

data class ShortVideoResponse(
    @SerializedName("short_videos")
    val shortVideos: List<ShortVideoItem> = emptyList()
)

data class ShortVideoItem(
    val title: String,
    val link: String,
    val source: String,
    val thumbnail: String,
    val extensions: List<String>? = null
)
