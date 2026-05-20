package com.example.serviceandroid.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.example.serviceandroid.helper.Constants
import com.example.serviceandroid.model.Action
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.service.MusicService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicServiceConnector @Inject constructor() {

    private var musicBinder: MusicService.MusicBinder? = null
    private var isBound: Boolean = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicBinder = service as MusicService.MusicBinder
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicBinder = null
        }
    }

    fun bind(context: Context) {
        if (isBound) return
        val ctx = context.applicationContext
        val intent = Intent(ctx, MusicService::class.java)
        ctx.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        isBound = true
    }

    fun unbind(context: Context) {
        if (!isBound) return
        val ctx = context.applicationContext
        try {
            ctx.unbindService(connection)
        } catch (_: IllegalArgumentException) {
        }
        isBound = false
        musicBinder = null
    }

    private fun dispatchAction(context: Context, action: Action) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, MusicService::class.java).apply {
                putExtra(Constants.RECEIVER_ACTION_MUSIC, action)
            }
        )
    }

    private fun dispatchSeek(context: Context, positionMs: Int) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, MusicService::class.java).apply {
                putExtra(Constants.EXTRA_SEEK_POSITION_MS, positionMs)
            }
        )
    }

    fun playSong(context: Context, song: Song) {
        val binder = musicBinder
        if (binder != null) {
            binder.playSong(song)
        } else {
            ContextCompat.startForegroundService(
                context,
                Intent(context, MusicService::class.java).apply {
                    putExtra(Constants.EXTRA_START_SONG, song)
                }
            )
        }
    }

    fun pause(context: Context) {
        musicBinder?.pause() ?: dispatchAction(context, Action.ACTION_PAUSE)
    }

    fun resume(context: Context) {
        musicBinder?.resume() ?: dispatchAction(context, Action.ACTION_RESUME)
    }

    fun next(context: Context) {
        musicBinder?.next() ?: dispatchAction(context, Action.ACTION_NEXT)
    }

    fun previous(context: Context) {
        musicBinder?.previous() ?: dispatchAction(context, Action.ACTION_PREVIOUS)
    }

    fun clear(context: Context) {
        musicBinder?.clear() ?: dispatchAction(context, Action.ACTION_CLEAR)
    }

    fun seekTo(context: Context, positionMs: Int) {
        musicBinder?.seekTo(positionMs) ?: dispatchSeek(context, positionMs)
    }

    fun syncRepeatMode(context: Context) {
        musicBinder?.syncRepeatFromPrefs() ?: dispatchAction(context, Action.ACTION_SYNC_REPEAT)
    }
}
