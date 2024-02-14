package com.example.serviceandroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MyReceiver : BroadcastReceiver() {
    companion object {
        const val RECEIVER_ACTION_MUSIC = "RECEIVER_ACTION_MUSIC"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val actionMusic = intent?.getIntExtra(HelloService.ACTION_MUSIC, 0)

        val intentService = Intent(context, HelloService::class.java)
        intentService.putExtra(RECEIVER_ACTION_MUSIC, actionMusic)
        context?.startService(intentService)
    }
}