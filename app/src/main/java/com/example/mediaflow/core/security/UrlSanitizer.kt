package com.example.mediaflow.core.security

import com.example.mediaflow.domain.model.Platform
import java.net.URI

object UrlSanitizer {

    private val YOUTUBE_HOSTS = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "youtu.be",
        "music.youtube.com"
    )

    private val INSTAGRAM_HOSTS = setOf(
        "instagram.com",
        "www.instagram.com"
    )

    // Tracking query parameters to strip
    private val TRACKING_PARAMS = setOf(
        "si", "feature", "igsh", "utm_source", "utm_medium", "utm_campaign",
        "utm_term", "utm_content", "fbclid", "gclid", "app"
    )

    fun isSupportedUrl(rawUrl: String): Boolean {
        return detectPlatform(rawUrl) != null
    }

    fun detectPlatform(rawUrl: String): Platform? {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return null

        val uri = try {
            val withScheme = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else {
                trimmed
            }
            URI(withScheme)
        } catch (e: Exception) {
            return null
        }

        val host = uri.host?.lowercase() ?: return null
        val path = uri.path ?: ""

        if (YOUTUBE_HOSTS.contains(host)) {
            if (host == "youtu.be" && path.length > 1) {
                return Platform.YOUTUBE
            }
            if (path.startsWith("/watch") || path.startsWith("/shorts/") || path.startsWith("/live/")) {
                return Platform.YOUTUBE
            }
        }

        if (INSTAGRAM_HOSTS.contains(host)) {
            if (path.startsWith("/p/") || path.startsWith("/reel/") || path.startsWith("/tv/") || path.startsWith("/reels/")) {
                return Platform.INSTAGRAM
            }
        }

        return null
    }

    /**
     * Sanitizes and enforces HTTPS, strips tracking query parameters, and validates format.
     */
    fun sanitizeUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        val platform = detectPlatform(trimmed) ?: return null

        val withHttps = if (trimmed.startsWith("http://")) {
            "https://" + trimmed.removePrefix("http://")
        } else if (!trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }

        return try {
            val uri = URI(withHttps)
            val host = uri.host?.lowercase() ?: return null
            val path = uri.path ?: ""

            // Strip tracking query parameters
            val rawQuery = uri.query
            val cleanQuery = if (rawQuery.isNullOrBlank()) {
                null
            } else {
                val pairs = rawQuery.split("&")
                val kept = pairs.filter { pair ->
                    val key = pair.substringBefore("=").lowercase()
                    !TRACKING_PARAMS.contains(key)
                }
                if (kept.isEmpty()) null else kept.joinToString("&")
            }

            val queryPart = if (cleanQuery != null) "?$cleanQuery" else ""
            "https://$host$path$queryPart"
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extracts YouTube video ID if present.
     */
    fun extractYouTubeId(sanitizedUrl: String): String? {
        return try {
            val uri = URI(sanitizedUrl)
            val host = uri.host?.lowercase() ?: return null
            if (host == "youtu.be") {
                uri.path?.removePrefix("/")?.take(11)
            } else if (uri.path?.startsWith("/shorts/") == true) {
                uri.path?.removePrefix("/shorts/")?.substringBefore("/")?.substringBefore("?")
            } else {
                val query = uri.query ?: return null
                query.split("&")
                    .firstOrNull { it.startsWith("v=") }
                    ?.removePrefix("v=")
                    ?.take(11)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extracts Instagram shortcode if present.
     */
    fun extractInstagramShortcode(sanitizedUrl: String): String? {
        return try {
            val uri = URI(sanitizedUrl)
            val path = uri.path ?: return null
            val segments = path.split("/").filter { it.isNotBlank() }
            if (segments.size >= 2 && (segments[0] == "p" || segments[0] == "reel" || segments[0] == "tv" || segments[0] == "reels")) {
                segments[1]
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
