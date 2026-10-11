package com.nichita.swearingtaskapp.data.remote

import kotlinx.serialization.Serializable

/** Proxy contract: POST {BASE_URL}/roast. `level` is SOFT/MEDIUM/HARD, or PRAISE for praise. */
@Serializable
data class RoastRequestDto(
    val level: String,
    val tasks: List<String>,
    val idleHours: Int,
    val doneCount: Int,
    val totalCount: Int,
    /** Profanity allowed. */
    val explicit: Boolean = false,
    /** Name-calling allowed (implies [explicit]). */
    val personal: Boolean = false,
)

@Serializable
data class RoastResponseDto(val text: String)

/** Proxy endpoint and app token from local.properties (BuildConfig). */
data class ProxyConfig(val baseUrl: String, val appToken: String) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && appToken.isNotBlank()
}
