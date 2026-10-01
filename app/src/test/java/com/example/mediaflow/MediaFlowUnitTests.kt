package com.example.mediaflow

import com.example.mediaflow.core.security.FilenameSanitizer
import com.example.mediaflow.core.security.UrlSanitizer
import com.example.mediaflow.domain.model.DownloadItem
import com.example.mediaflow.domain.model.DownloadStatus
import com.example.mediaflow.domain.model.MediaType
import com.example.mediaflow.domain.model.Platform
import com.example.mediaflow.domain.usecase.ValidateUrlUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaFlowUnitTests {

    @Test
    fun testYouTubePlatformDetection() {
        val watchUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val shortUrl = "https://youtu.be/dQw4w9WgXcQ"
        val shortsUrl = "https://youtube.com/shorts/dQw4w9WgXcQ?si=abcdef"

        assertEquals(Platform.YOUTUBE, UrlSanitizer.detectPlatform(watchUrl))
        assertEquals(Platform.YOUTUBE, UrlSanitizer.detectPlatform(shortUrl))
        assertEquals(Platform.YOUTUBE, UrlSanitizer.detectPlatform(shortsUrl))
    }

    @Test
    fun testInstagramPlatformDetection() {
        val postUrl = "https://www.instagram.com/p/C31234abc/"
        val reelUrl = "https://instagram.com/reel/C45678xyz/?igsh=tracking123"
        val tvUrl = "https://www.instagram.com/tv/C99999abc/"

        assertEquals(Platform.INSTAGRAM, UrlSanitizer.detectPlatform(postUrl))
        assertEquals(Platform.INSTAGRAM, UrlSanitizer.detectPlatform(reelUrl))
        assertEquals(Platform.INSTAGRAM, UrlSanitizer.detectPlatform(tvUrl))
    }

    @Test
    fun testUnsupportedUrlDetection() {
        assertNull(UrlSanitizer.detectPlatform("https://example.com/video.mp4"))
        assertNull(UrlSanitizer.detectPlatform("https://twitter.com/user/status/123"))
        assertNull(UrlSanitizer.detectPlatform("invalid-string"))
        assertNull(UrlSanitizer.detectPlatform(""))
    }

    @Test
    fun testUrlSanitizerStripsTrackingParameters() {
        val urlWithTracking = "https://youtube.com/watch?v=dQw4w9WgXcQ&si=tracking123&utm_source=twitter"
        val sanitized = UrlSanitizer.sanitizeUrl(urlWithTracking)

        assertNotNull(sanitized)
        assertTrue(sanitized!!.contains("v=dQw4w9WgXcQ"))
        assertFalse(sanitized.contains("si=tracking123"))
        assertFalse(sanitized.contains("utm_source"))
    }

    @Test
    fun testExtractYouTubeId() {
        val id1 = UrlSanitizer.extractYouTubeId("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        val id2 = UrlSanitizer.extractYouTubeId("https://youtu.be/dQw4w9WgXcQ")
        val id3 = UrlSanitizer.extractYouTubeId("https://youtube.com/shorts/dQw4w9WgXcQ")

        assertEquals("dQw4w9WgXcQ", id1)
        assertEquals("dQw4w9WgXcQ", id2)
        assertEquals("dQw4w9WgXcQ", id3)
    }

    @Test
    fun testExtractInstagramShortcode() {
        val sc1 = UrlSanitizer.extractInstagramShortcode("https://www.instagram.com/p/CxY123/")
        val sc2 = UrlSanitizer.extractInstagramShortcode("https://instagram.com/reel/CzA789/")

        assertEquals("CxY123", sc1)
        assertEquals("CzA789", sc2)
    }

    @Test
    fun testFilenameSanitizerRemovesIllegalCharacters() {
        val rawTitle = "My Video: Test? 2026"
        val sanitized = FilenameSanitizer.sanitize(rawTitle, "mp4")

        assertEquals("My Video Test 2026.mp4", sanitized)
    }

    @Test
    fun testFilenameSanitizerPreventsPathTraversal() {
        val rawTitle = "../../etc/passwd"
        val sanitized = FilenameSanitizer.sanitize(rawTitle, "mp4")

        assertFalse(sanitized.contains(".."))
        assertFalse(sanitized.contains("/"))
    }

    @Test
    fun testValidateUrlUseCase() {
        val useCase = ValidateUrlUseCase()

        val valid = useCase("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertTrue(valid is ValidateUrlUseCase.ValidationResult.Valid)

        val invalid = useCase("https://unknown.com/file")
        assertTrue(invalid is ValidateUrlUseCase.ValidationResult.Invalid)

        val empty = useCase("   ")
        assertTrue(empty is ValidateUrlUseCase.ValidationResult.Invalid)
    }

    @Test
    fun testDownloadItemProgressCalculation() {
        val item = DownloadItem(
            id = "test-1",
            originalUrl = "https://youtube.com/watch?v=123",
            platform = Platform.YOUTUBE,
            title = "Test Video",
            mediaType = MediaType.VIDEO,
            format = "mp4",
            quality = "1080p",
            fileName = "Test Video.mp4",
            totalBytes = 100_000L,
            downloadedBytes = 50_000L,
            status = DownloadStatus.DOWNLOADING
        )

        assertEquals(0.5f, item.progressFloat, 0.001f)
        assertEquals(50, item.progressPercentage)
        assertTrue(item.status.isActive)
        assertFalse(item.status.isFinished)
    }
}
