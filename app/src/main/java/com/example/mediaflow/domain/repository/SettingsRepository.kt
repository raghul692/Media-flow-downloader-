package com.example.mediaflow.domain.repository

import com.example.mediaflow.domain.model.AppSettings
import com.example.mediaflow.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setWifiOnly(enabled: Boolean)
    suspend fun setDefaultVideoQuality(quality: String)
    suspend fun setDefaultAudioQuality(quality: String)
    suspend fun setMaxConcurrentDownloads(max: Int)
    suspend fun setPolicyAgreed(agreed: Boolean)
}
