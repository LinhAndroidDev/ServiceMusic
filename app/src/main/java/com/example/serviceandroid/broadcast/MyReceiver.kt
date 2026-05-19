package com.example.serviceandroid.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.serviceandroid.model.Action
import com.example.serviceandroid.helper.Constants
import com.example.serviceandroid.service.MusicService

@Suppress("DEPRECATION")
class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val actionMusic = readAction(intent) ?: return
        val intentService = Intent(context, MusicService::class.java).apply {
            putExtra(Constants.RECEIVER_ACTION_MUSIC, actionMusic)
        }
        ContextCompat.startForegroundService(context!!, intentService)
    }

    private fun readAction(intent: Intent?): Action? {
        if (intent == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(Constants.ACTION_MUSIC, Action::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(Constants.ACTION_MUSIC) as? Action
        }
    }
}
