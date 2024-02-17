package com.example.serviceandroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val actionMusic = intent?.getSerializableExtra(Constants.ACTION_MUSIC) as Action

        val intentService = Intent(context, HelloService::class.java)
        intentService.putExtra(Constants.RECEIVER_ACTION_MUSIC, actionMusic)
        context?.startService(intentService)
    }
}