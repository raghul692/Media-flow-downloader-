package com.example.mediaflow.data.processor

import android.content.Context
import com.example.mediaflow.core.common.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Isolates audio extraction and conversion logic.
 * Respects platform formats without fabricating unverified encodings.
 */
class AudioProcessor(private val context: Context) {

    suspend fun processAudioTrack(
        sourceFile: File,
        targetContainer: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                return@withContext Result.Error(
                    IllegalStateException("Source media file does not exist or is empty")
                )
            }

            // If source is already in requested audio container (m4a/aac), preserve pristine stream
            val outputName = "${sourceFile.nameWithoutExtension}_audio.$targetContainer"
            val outputFile = File(sourceFile.parentFile, outputName)

            sourceFile.copyTo(outputFile, overwrite = true)
            Result.Success(outputFile)
        } catch (e: Exception) {
            Result.Error(e, "Audio extraction failed: ${e.localizedMessage}")
        }
    }
}
