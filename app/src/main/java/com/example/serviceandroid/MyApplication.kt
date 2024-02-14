package com.example.serviceandroid

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MyApplication : Application() {

    companion object {
        const val CHANNEL_ID = "CHANNEL_ID"
    }

    override fun onCreate() {
        super.onCreate()

        createChannelNotification()
    }

    private fun createChannelNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "channel id",
                NotificationManager.IMPORTANCE_MIN
            )
            channel.setSound(null, null)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }
}