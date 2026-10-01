package com.example.mediaflow.domain.model

enum class DownloadStatus(val displayName: String) {
    QUEUED("Queued"),
    ANALYZING("Analyzing"),
    DOWNLOADING("Downloading"),
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled"),
    PAUSED("Paused");

    val isActive: Boolean get() = this == QUEUED || this == ANALYZING || this == DOWNLOADING || this == PROCESSING || this == PAUSED
    val isFinished: Boolean get() = this == COMPLETED || this == FAILED || this == CANCELLED
}
