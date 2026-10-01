package com.example.mediaflow.core.security

import java.util.regex.Pattern

object FilenameSanitizer {

    private val ILLEGAL_CHARS = Pattern.compile("[/\\\\:?*\"<>|\\p{Cntrl}]")
    private val MULTI_SPACE = Pattern.compile("\\s+")

    fun sanitize(title: String, extension: String): String {
        val ext = extension.removePrefix(".").trim()
        val cleanExt = if (ext.isNotBlank()) ".$ext" else ""

        // Strip illegal characters
        var cleaned = ILLEGAL_CHARS.matcher(title).replaceAll("")
        // Normalize whitespace
        cleaned = MULTI_SPACE.matcher(cleaned).replaceAll(" ").trim()

        // Strip leading dots and path traversal attempts
        cleaned = cleaned.replace("..", "").trimStart('.')

        if (cleaned.isBlank()) {
            cleaned = "MediaFlow_${System.currentTimeMillis()}"
        }

        // Maximum filename length in standard Linux/Android filesystem is 255 bytes
        val maxBaseLength = 120
        if (cleaned.length > maxBaseLength) {
            cleaned = cleaned.take(maxBaseLength).trim()
        }

        return "$cleaned$cleanExt"
    }
}
