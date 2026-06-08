package com.streamlux.app.data

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isPremium: Boolean = false,
    val bookmarks: List<String> = emptyList()
)
