package com.example.serviceandroid.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.serviceandroid.model.Action
import com.example.serviceandroid.helper.Constants
import com.example.serviceandroid.service.HelloService

@Suppress("DEPRECATION")
class MyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val actionMusic = intent?.getSerializableExtra(Constants.ACTION_MUSIC) as Action
        when(actionMusic) {
            Action.ACTION_PAUSE -> {

            }
            Action.ACTION_RESUME -> {

            }
            Action.ACTION_CLEAR -> {

            }
            Action.ACTION_START -> {

            }
            Action.ACTION_NEXT -> {

            }
            Action.ACTION_PREVIOUS -> {

            }
            Action.ACTION_FINISH -> {

            }

        }

        val intentService = Intent(context, HelloService::class.java)
        intentService.putExtra(Constants.RECEIVER_ACTION_MUSIC, actionMusic)
        context?.startService(intentService)
    }
}