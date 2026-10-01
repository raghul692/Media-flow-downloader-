package com.example.mediaflow.di

import android.content.Context
import com.example.mediaflow.data.database.MediaFlowDatabase
import com.example.mediaflow.data.preferences.SettingsPreferencesDataSource
import com.example.mediaflow.data.processor.AudioProcessor
import com.example.mediaflow.data.provider.MediaProvider
import com.example.mediaflow.data.provider.MediaResolver
import com.example.mediaflow.data.provider.instagram.InstagramMediaProvider
import com.example.mediaflow.data.provider.youtube.YouTubeMediaProvider
import com.example.mediaflow.data.repository.DownloadRepositoryImpl
import com.example.mediaflow.data.repository.HistoryRepositoryImpl
import com.example.mediaflow.data.repository.MediaRepositoryImpl
import com.example.mediaflow.data.repository.SettingsRepositoryImpl
import com.example.mediaflow.data.storage.MediaStoreRepositoryImpl
import com.example.mediaflow.domain.model.Platform
import com.example.mediaflow.domain.repository.DownloadRepository
import com.example.mediaflow.domain.repository.HistoryRepository
import com.example.mediaflow.domain.repository.MediaRepository
import com.example.mediaflow.domain.repository.SettingsRepository
import com.example.mediaflow.domain.repository.StorageRepository
import com.example.mediaflow.domain.usecase.AnalyzeMediaUseCase
import com.example.mediaflow.domain.usecase.DownloadUseCases
import com.example.mediaflow.domain.usecase.GetHistoryUseCase
import com.example.mediaflow.domain.usecase.LibraryUseCases
import com.example.mediaflow.domain.usecase.ValidateUrlUseCase
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(private val context: Context) {

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    val database: MediaFlowDatabase by lazy {
        MediaFlowDatabase.getInstance(context)
    }

    val preferencesDataSource: SettingsPreferencesDataSource by lazy {
        SettingsPreferencesDataSource(context)
    }

    val audioProcessor: AudioProcessor by lazy {
        AudioProcessor(context)
    }

    private val youtubeProvider: MediaProvider by lazy {
        YouTubeMediaProvider(okHttpClient)
    }

    private val instagramProvider: MediaProvider by lazy {
        InstagramMediaProvider(okHttpClient)
    }

    val mediaResolver: MediaResolver by lazy {
        MediaResolver(
            mapOf(
                Platform.YOUTUBE to youtubeProvider,
                Platform.INSTAGRAM to instagramProvider
            )
        )
    }

    val mediaRepository: MediaRepository by lazy {
        MediaRepositoryImpl(mediaResolver)
    }

    val downloadRepository: DownloadRepository by lazy {
        DownloadRepositoryImpl(context, database.downloadHistoryDao(), preferencesDataSource)
    }

    val historyRepository: HistoryRepository by lazy {
        HistoryRepositoryImpl(database.downloadHistoryDao())
    }

    val storageRepository: StorageRepository by lazy {
        MediaStoreRepositoryImpl(context)
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(preferencesDataSource)
    }

    // UseCases
    val validateUrlUseCase by lazy { ValidateUrlUseCase() }
    val analyzeMediaUseCase by lazy { AnalyzeMediaUseCase(mediaRepository, validateUrlUseCase) }
    val downloadUseCases by lazy { DownloadUseCases(downloadRepository) }
    val libraryUseCases by lazy { LibraryUseCases(storageRepository) }
    val getHistoryUseCase by lazy { GetHistoryUseCase(historyRepository) }
}
