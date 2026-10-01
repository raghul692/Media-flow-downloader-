package com.example.mediaflow.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val wifiOnly: Boolean = false,
    val defaultVideoQuality: String = "1080p",
    val defaultAudioQuality: String = "256 kbps",
    val maxConcurrentDownloads: Int = 3,
    val hasAgreedToPolicy: Boolean = false
)
