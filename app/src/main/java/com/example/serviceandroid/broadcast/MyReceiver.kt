package com.example.serviceandroid.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.serviceandroid.model.Action
import com.example.serviceandroid.helper.Constants
import com.example.serviceandroid.service.HelloService

class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val actionMusic = intent?.getSerializableExtra(Constants.ACTION_MUSIC) as Action

        val intentService = Intent(context, HelloService::class.java)
        intentService.putExtra(Constants.RECEIVER_ACTION_MUSIC, actionMusic)
        context?.startService(intentService)
    }
}