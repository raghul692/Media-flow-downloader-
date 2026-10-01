package com.example.mediaflow.domain.model

data class MediaFormat(
    val id: String,
    val mediaType: MediaType,
    val container: String, // e.g. "mp4", "m4a", "webm"
    val qualityLabel: String, // e.g. "1080p", "720p", "480p", "128 kbps", "256 kbps"
    val resolutionWidth: Int? = null,
    val resolutionHeight: Int? = null,
    val bitrateKbps: Int? = null,
    val estimatedSizeBytes: Long = 0L,
    val downloadUrl: String? = null
)
