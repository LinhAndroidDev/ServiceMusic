package com.example.serviceandroid

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.serviceandroid.databinding.ActivityMainBinding
import com.example.serviceandroid.model.Action
import com.example.serviceandroid.helper.Constants
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.service.HelloService

class MainActivity : AppCompatActivity() {
    private lateinit var mSong: Song
    private var isPlaying: Boolean = false
    private lateinit var progressMusic: CountDownTimer
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mSong = intent.getSerializableExtra(Constants.OBJECT_SONG) as Song
            isPlaying = intent.getBooleanExtra(Constants.STATUS_PLAYING, false)
            handleLayoutMusic(intent.getSerializableExtra(Constants.ACTION_MUSIC) as Action)
        }

    }

    companion object {
        const val MESSAGE_MAIN = "MESSAGE_MAIN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, IntentFilter(Constants.SEND_DATA_TO_ACTIVITY))

        binding.startMusic.setOnClickListener {
            clickStartService()
        }

        binding.stopMusic.setOnClickListener {
            sendActionToService(Action.ACTION_CLEAR)
        }
    }

    private fun clickStopService() {
        val intent = Intent(this, HelloService::class.java)
        stopService(intent)
    }

    @SuppressLint("SetTextI18n")
    private fun clickStartService() {
        val intent = Intent(this, HelloService::class.java)
        val song = Song("Lạ Lùng", "Vũ", R.drawable.la_lung, R.raw.la_lung, 262)
        intent.putExtra(MESSAGE_MAIN, song)
        startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    private fun handleLayoutMusic(action: Action) {
        when (action) {
            Action.ACTION_START -> {
                binding.bottomPlay.visibility = View.VISIBLE
                showInfoSong()
                setStatusButtonPlay()
            }

            Action.ACTION_RESUME -> {
                progressMusic.start()
                setStatusButtonPlay()
            }

            Action.ACTION_PAUSE -> {
                progressMusic.cancel()
                setStatusButtonPlay()
            }

            else -> {
                binding.bottomPlay.visibility = View.GONE
                progressMusic.cancel()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showInfoSong() {
        binding.avatar.setImageResource(mSong.avatar)
        binding.title.text = mSong.title
        binding.nameSingle.text = "Ca sĩ: ${mSong.name}"
        binding.bottomPlay.visibility = View.VISIBLE
        binding.progressMusic.max = mSong.time
        binding.progressMusic.progress = 0

        progressMusic = object : CountDownTimer(((mSong.time * 1000).toLong()), 1000) {
            override fun onTick(p0: Long) {
                binding.progressMusic.progress += 1
            }

            override fun onFinish() {
                sendActionToService(Action.ACTION_PAUSE)
            }

        }.start()

        binding.play.setOnClickListener {
            if (binding.progressMusic.progress == binding.progressMusic.max && !isPlaying) {
                sendActionToService(Action.ACTION_START)
            } else {
                sendActionToService(if (isPlaying) Action.ACTION_PAUSE else Action.ACTION_RESUME)
            }
        }

        binding.close.setOnClickListener {
            sendActionToService(Action.ACTION_CLEAR)
        }
    }

    private fun setStatusButtonPlay() {
        binding.play.setImageResource(
            if (isPlaying) R.drawable.pause else R.drawable.play
        )
    }

    private fun sendActionToService(action: Action) {
        val intent = Intent(this, HelloService::class.java)
        intent.putExtra(Constants.RECEIVER_ACTION_MUSIC, action)

        startService(intent)
    }
}