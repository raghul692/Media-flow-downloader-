package com.example.mediaflow.domain.usecase

import com.example.mediaflow.core.security.UrlSanitizer
import com.example.mediaflow.domain.model.Platform

class ValidateUrlUseCase {

    operator fun invoke(url: String): ValidationResult {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            return ValidationResult.Invalid("Please enter a YouTube or Instagram URL.")
        }

        val platform = UrlSanitizer.detectPlatform(trimmed)
            ?: return ValidationResult.Invalid("Please enter a valid YouTube or Instagram URL.")

        val sanitized = UrlSanitizer.sanitizeUrl(trimmed)
            ?: return ValidationResult.Invalid("Unable to normalize the entered URL.")

        return ValidationResult.Valid(sanitized, platform)
    }

    sealed class ValidationResult {
        data class Valid(val sanitizedUrl: String, val platform: Platform) : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}
