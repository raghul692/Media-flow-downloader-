package com.example.mediaflow.data.provider

import com.example.mediaflow.core.common.Result
import com.example.mediaflow.core.security.UrlSanitizer
import com.example.mediaflow.domain.model.MediaMetadata
import com.example.mediaflow.domain.model.Platform

class MediaResolver(
    private val providers: Map<Platform, MediaProvider>
) {
    suspend fun resolve(sanitizedUrl: String): Result<MediaMetadata> {
        val platform = UrlSanitizer.detectPlatform(sanitizedUrl)
            ?: return Result.Error(
                IllegalArgumentException("Unsupported platform"),
                "The provided URL is not a supported YouTube or Instagram link."
            )

        val provider = providers[platform]
            ?: return Result.Error(
                IllegalStateException("No provider registered for platform: $platform"),
                "Media provider for ${platform.displayName} is not configured."
            )

        return provider.resolveMedia(sanitizedUrl)
    }
}
