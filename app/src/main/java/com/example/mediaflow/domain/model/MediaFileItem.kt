package com.example.mediaflow.domain.model

data class MediaFileItem(
    val id: Long,
    val contentUri: String,
    val name: String,
    val mediaType: MediaType,
    val sizeBytes: Long,
    val durationMs: Long = 0L,
    val dateAddedSec: Long = 0L,
    val mimeType: String,
    val platform: Platform? = null,
    val filePath: String? = null
)
