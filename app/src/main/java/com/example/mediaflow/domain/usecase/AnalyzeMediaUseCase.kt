package com.example.mediaflow.domain.usecase

import com.example.mediaflow.core.common.Result
import com.example.mediaflow.domain.model.MediaMetadata
import com.example.mediaflow.domain.repository.MediaRepository

class AnalyzeMediaUseCase(
    private val mediaRepository: MediaRepository,
    private val validateUrlUseCase: ValidateUrlUseCase
) {
    suspend operator fun invoke(url: String): Result<MediaMetadata> {
        return when (val validation = validateUrlUseCase(url)) {
            is ValidateUrlUseCase.ValidationResult.Invalid -> {
                Result.Error(IllegalArgumentException(validation.reason), validation.reason)
            }
            is ValidateUrlUseCase.ValidationResult.Valid -> {
                mediaRepository.analyzeUrl(validation.sanitizedUrl)
            }
        }
    }
}
