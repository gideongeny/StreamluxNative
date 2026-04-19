package com.streamlux.app.data.model

data class TVChannel(
    val id: String,
    val name: String,
    val type: String,
    val url: String,
    val category: String,
    val logo: String? = null,
    val isExternal: Boolean = false,
    val country: String? = null,
    val countryCode: String? = null
)
