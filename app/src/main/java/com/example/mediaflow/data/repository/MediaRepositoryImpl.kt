package com.example.mediaflow.data.repository

import com.example.mediaflow.core.common.Result
import com.example.mediaflow.core.security.UrlSanitizer
import com.example.mediaflow.data.provider.MediaResolver
import com.example.mediaflow.domain.model.MediaMetadata
import com.example.mediaflow.domain.model.Platform
import com.example.mediaflow.domain.repository.MediaRepository

class MediaRepositoryImpl(
    private val mediaResolver: MediaResolver
) : MediaRepository {

    override suspend fun analyzeUrl(url: String): Result<MediaMetadata> {
        val sanitized = UrlSanitizer.sanitizeUrl(url)
            ?: return Result.Error(
                IllegalArgumentException("Invalid URL"),
                "Please enter a valid YouTube or Instagram URL."
            )

        return mediaResolver.resolve(sanitized)
    }

    override fun validateAndDetectPlatform(url: String): Platform? {
        return UrlSanitizer.detectPlatform(url)
    }
}
