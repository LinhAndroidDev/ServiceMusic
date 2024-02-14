package com.example.serviceandroid

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

class HelloService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private lateinit var mSong: Song

    companion object {
        const val ACTION_MUSIC = "ACTION_MUSIC"
        private const val ACTION_PAUSE = 1
        private const val ACTION_RESUME = 2
        private const val ACTION_CLEAR = 3
    }

    override fun onCreate() {
        super.onCreate()

        Log.e("Linh: ", "onCreate")
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getSerializableExtra(MainActivity.MESSAGE_MAIN).let {
            if(it != null) {
                val song = it as Song
                mSong = song
                mediaPlayer = MediaPlayer.create(this, song.sing)
                mediaPlayer?.start()
                isPlaying = true
                sendNotification(song)
            }
        }

        val actionMusic = intent!!.getIntExtra(MyReceiver.RECEIVER_ACTION_MUSIC, 0)
        handleActionMusic(actionMusic)
        return START_NOT_STICKY
    }

    @SuppressLint("ForegroundServiceType")
    private fun sendNotification(song: Song) {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)

        val remoteViews = RemoteViews(packageName, R.layout.layout_custom_music)
        remoteViews.setTextViewText(R.id.title, song.title)
        remoteViews.setTextViewText(R.id.nameSingle, song.name + "(Single)")
        remoteViews.setImageViewResource(R.id.avatar, song.avatar)

        if(isPlaying && mediaPlayer != null) {
            remoteViews.setOnClickPendingIntent(R.id.play, getPendingIntent(this, ACTION_PAUSE))
            remoteViews.setImageViewResource(R.id.play, R.drawable.pause)
        } else {
            remoteViews.setOnClickPendingIntent(R.id.play, getPendingIntent(this, ACTION_RESUME))
            remoteViews.setImageViewResource(R.id.play, R.drawable.play)
        }

        remoteViews.setOnClickPendingIntent(R.id.close, getPendingIntent(this, ACTION_CLEAR))

        val notification = NotificationCompat.Builder(this, MyApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pending)
            .setCustomBigContentView(remoteViews)
            .build()

        startForeground(1, notification)
    }

    private fun getPendingIntent(context: Context, action: Int): PendingIntent? {
        val intent = Intent(this, MyReceiver::class.java)
        intent.putExtra(ACTION_MUSIC, action)

        return PendingIntent.getBroadcast(context.applicationContext, action, intent,
            PendingIntent.FLAG_MUTABLE)
    }

    private fun handleActionMusic(action: Int) {
        when (action) {
            ACTION_PAUSE -> {
                pauseMusic()
            }

            ACTION_CLEAR -> {
                stopSelf()
            }

            ACTION_RESUME -> {
                resumeMusic()
            }
        }
    }

    private fun resumeMusic() {
        if(!isPlaying) {
            mediaPlayer?.start()
            isPlaying = true
            sendNotification(mSong)
        }
    }

    private fun pauseMusic() {
        if(isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
            sendNotification(mSong)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.e("Linh: ", "onDestroy")
        mediaPlayer?.stop()
    }
}