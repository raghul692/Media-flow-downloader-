package com.example.mediaflow.domain.repository

import com.example.mediaflow.core.common.Result
import com.example.mediaflow.domain.model.MediaMetadata
import com.example.mediaflow.domain.model.Platform

interface MediaRepository {
    suspend fun analyzeUrl(url: String): Result<MediaMetadata>
    fun validateAndDetectPlatform(url: String): Platform?
}
