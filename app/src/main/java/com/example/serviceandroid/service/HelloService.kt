package com.example.serviceandroid.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.serviceandroid.model.Action
import com.example.serviceandroid.helper.Constants
import com.example.serviceandroid.MainActivity
import com.example.serviceandroid.helper.MyApplication
import com.example.serviceandroid.broadcast.MyReceiver
import com.example.serviceandroid.R
import com.example.serviceandroid.model.Song

@Suppress("DEPRECATION")
class HelloService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private lateinit var mSong: Song

    override fun onCreate() {
        super.onCreate()

        Log.e("Linh: ", "onCreate")
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getParcelableExtra<Song>(MainActivity.MESSAGE_MAIN)?.let {
            mSong = it
            startMusic(mSong)
            sendNotificationMediaType(mSong)
        }

        intent?.getSerializableExtra(Constants.RECEIVER_ACTION_MUSIC)?.let {
            handleActionMusic(it as Action)
        }
        return START_NOT_STICKY
    }

    @SuppressLint("ForegroundServiceType")
    private fun sendNotificationMediaType(song: Song) {
        val bitmap = BitmapFactory.decodeResource(resources, song.avatar)
        val mediaSessionCompat = MediaSessionCompat(this, "tag")

        val notification = NotificationCompat.Builder(this, MyApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.music)
            .setSubText("Linh Nguyen")
            .setContentTitle(song.title)
            .setContentText("Ca sĩ: ${song.nameSinger}")
            .setLargeIcon(bitmap)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSessionCompat.sessionToken)
            )

        if (isPlaying && mediaPlayer != null) {
            notification
                .addAction(R.drawable.skip_previous, "Previous", null) // #0
                .addAction(
                    R.drawable.pause,
                    "Pause",
                    getPendingIntent(this, Action.ACTION_PAUSE)
                ) // #1
                .addAction(R.drawable.skip_next, "Next", null) // #2
                .setProgress(mediaPlayer!!.duration, mediaPlayer!!.currentPosition, false)
        } else {
            notification
                .addAction(R.drawable.skip_previous, "Previous", null) // #0
                .addAction(
                    R.drawable.play,
                    "Pause",
                    getPendingIntent(this, Action.ACTION_RESUME)
                ) // #1
                .addAction(R.drawable.skip_next, "Next", null) // #2
                .setProgress(mediaPlayer!!.duration, mediaPlayer!!.currentPosition, false)
        }

        startForeground(1, notification.build())
    }

    private fun getPendingIntent(context: Context, action: Action): PendingIntent? {
        val intent = Intent(this, MyReceiver::class.java)
        intent.putExtra(Constants.ACTION_MUSIC, action)

        return PendingIntent.getBroadcast(
            context.applicationContext, action.ordinal, intent,
            PendingIntent.FLAG_MUTABLE
        )
    }

    private fun handleActionMusic(action: Action) {
        when (action) {
            Action.ACTION_PAUSE -> {
                pauseMusic()
            }

            Action.ACTION_CLEAR -> {
                stopSelf()
                sendActionToActivity(Action.ACTION_CLEAR)
            }

            Action.ACTION_RESUME -> {
                resumeMusic()
            }

            else -> {
                startMusic(mSong)
            }
        }
    }

    private fun startMusic(song: Song) {
        mediaPlayer = MediaPlayer.create(this, song.sing)
        mediaPlayer?.start()
        isPlaying = true
        sendActionToActivity(Action.ACTION_START)
    }

    private fun resumeMusic() {
        if (!isPlaying) {
            mediaPlayer?.start()
            isPlaying = true
            sendNotificationMediaType(mSong)
            sendActionToActivity(Action.ACTION_RESUME)
        }
    }

    private fun pauseMusic() {
        if (isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
            sendNotificationMediaType(mSong)
            sendActionToActivity(Action.ACTION_PAUSE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.e("Linh: ", "onDestroy")
        mediaPlayer?.stop()
    }

    private fun sendActionToActivity(action: Action) {
        val intent = Intent(Constants.SEND_DATA_TO_ACTIVITY)
        intent.putExtra(Constants.OBJECT_SONG, mSong)
        intent.putExtra(Constants.STATUS_PLAYING, isPlaying)
        intent.putExtra(Constants.ACTION_MUSIC, action)

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}