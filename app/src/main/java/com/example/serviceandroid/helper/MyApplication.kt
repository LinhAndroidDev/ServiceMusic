package com.example.serviceandroid.helper

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    companion object {
        /** New id so importance upgrade applies without clearing app data. */
        const val CHANNEL_ID = "CHANNEL_MEDIA_PLAYBACK"
        const val DOWNLOAD_CHANNEL_ID = "CHANNEL_SONG_DOWNLOAD"
    }

    override fun onCreate() {
        super.onCreate()
        createChannelNotification()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createChannelNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // IMPORTANCE_MIN can hide or limit media notification actions on some devices/OS versions.
            val channel = NotificationChannel(
                CHANNEL_ID,
                "channel id",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setSound(null, null)
            val downloadChannel = NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                "Tải bài hát",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.apply {
                createNotificationChannel(channel)
                createNotificationChannel(downloadChannel)
            }
        }
    }
}
