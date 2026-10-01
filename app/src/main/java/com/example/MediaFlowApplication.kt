package com.example

import android.app.Application
import com.example.mediaflow.di.AppContainer
import com.example.mediaflow.worker.DownloadNotificationHelper

class MediaFlowApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        DownloadNotificationHelper.createNotificationChannel(this)
    }
}
