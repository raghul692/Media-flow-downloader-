package com.example.mediaflow.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mediaflow.domain.model.AppSettings
import com.example.mediaflow.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "mediaflow_settings")

class SettingsPreferencesDataSource(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val DEFAULT_VIDEO_QUALITY = stringPreferencesKey("default_video_quality")
        val DEFAULT_AUDIO_QUALITY = stringPreferencesKey("default_audio_quality")
        val MAX_CONCURRENT = intPreferencesKey("max_concurrent")
        val HAS_AGREED_POLICY = booleanPreferencesKey("has_agreed_to_policy")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val themeModeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeModeStr)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }

        AppSettings(
            themeMode = themeMode,
            wifiOnly = preferences[PreferencesKeys.WIFI_ONLY] ?: false,
            defaultVideoQuality = preferences[PreferencesKeys.DEFAULT_VIDEO_QUALITY] ?: "1080p",
            defaultAudioQuality = preferences[PreferencesKeys.DEFAULT_AUDIO_QUALITY] ?: "256 kbps",
            maxConcurrentDownloads = preferences[PreferencesKeys.MAX_CONCURRENT] ?: 3,
            hasAgreedToPolicy = preferences[PreferencesKeys.HAS_AGREED_POLICY] ?: false
        )
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIFI_ONLY] = enabled
        }
    }

    suspend fun setDefaultVideoQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_VIDEO_QUALITY] = quality
        }
    }

    suspend fun setDefaultAudioQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_AUDIO_QUALITY] = quality
        }
    }

    suspend fun setMaxConcurrentDownloads(max: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAX_CONCURRENT] = max
        }
    }

    suspend fun setPolicyAgreed(agreed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_AGREED_POLICY] = agreed
        }
    }
}
