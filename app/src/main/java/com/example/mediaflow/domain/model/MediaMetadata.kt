package com.example.mediaflow.domain.model

data class MediaMetadata(
    val id: String,
    val originalUrl: String,
    val sanitizedUrl: String,
    val platform: Platform,
    val title: String,
    val creator: String? = null,
    val durationSeconds: Long = 0L,
    val thumbnail: String? = null,
    val viewsCount: Long? = null,
    val isLive: Boolean = false,
    val isRestricted: Boolean = false,
    val restrictedReason: String? = null,
    val formats: List<MediaFormat> = emptyList()
)
