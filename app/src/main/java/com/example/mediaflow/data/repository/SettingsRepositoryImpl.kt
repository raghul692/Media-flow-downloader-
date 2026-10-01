package com.example.mediaflow.data.repository

import com.example.mediaflow.data.preferences.SettingsPreferencesDataSource
import com.example.mediaflow.domain.model.AppSettings
import com.example.mediaflow.domain.model.ThemeMode
import com.example.mediaflow.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val preferencesDataSource: SettingsPreferencesDataSource
) : SettingsRepository {

    override val settingsFlow: Flow<AppSettings> = preferencesDataSource.settingsFlow

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        preferencesDataSource.setThemeMode(themeMode)
    }

    override suspend fun setWifiOnly(enabled: Boolean) {
        preferencesDataSource.setWifiOnly(enabled)
    }

    override suspend fun setDefaultVideoQuality(quality: String) {
        preferencesDataSource.setDefaultVideoQuality(quality)
    }

    override suspend fun setDefaultAudioQuality(quality: String) {
        preferencesDataSource.setDefaultAudioQuality(quality)
    }

    override suspend fun setMaxConcurrentDownloads(max: Int) {
        preferencesDataSource.setMaxConcurrentDownloads(max)
    }

    override suspend fun setPolicyAgreed(agreed: Boolean) {
        preferencesDataSource.setPolicyAgreed(agreed)
    }
}
