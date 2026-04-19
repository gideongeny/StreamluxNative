package com.streamlux.app.data.model

data class Comment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "User",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val rating: Float = 0f
)
