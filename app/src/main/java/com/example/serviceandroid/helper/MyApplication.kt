package com.example.serviceandroid.helper

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {

    companion object {
        /** New id so importance upgrade applies without clearing app data. */
        const val CHANNEL_ID = "CHANNEL_MEDIA_PLAYBACK"
    }

    override fun onCreate() {
        super.onCreate()
        createChannelNotification()
    }

    private fun createChannelNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // IMPORTANCE_MIN can hide or limit media notification actions on some devices/OS versions.
            val channel = NotificationChannel(
                CHANNEL_ID,
                "channel id",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.setSound(null, null)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}