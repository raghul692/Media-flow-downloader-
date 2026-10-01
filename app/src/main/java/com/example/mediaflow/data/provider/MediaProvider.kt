package com.example.mediaflow.data.provider

import com.example.mediaflow.core.common.Result
import com.example.mediaflow.domain.model.MediaMetadata
import com.example.mediaflow.domain.model.Platform

interface MediaProvider {
    val platform: Platform
    suspend fun resolveMedia(sanitizedUrl: String): Result<MediaMetadata>
}
