package com.example.serviceandroid.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.example.serviceandroid.R
import com.example.serviceandroid.data.repository.SongRepository
import com.example.serviceandroid.helper.Constants
import com.example.serviceandroid.helper.MyApplication
import com.example.serviceandroid.model.Action
import com.example.serviceandroid.model.Repeat
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.playback.PlaybackStateHolder
import com.example.serviceandroid.playback.PlaybackUiState
import com.example.serviceandroid.utils.SharePreferenceRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@Suppress("DEPRECATION")
@AndroidEntryPoint
class MusicService : Service() {

    @Inject
    lateinit var playbackStateHolder: PlaybackStateHolder

    @Inject
    lateinit var songRepository: SongRepository

    @Inject
    lateinit var prefs: SharePreferenceRepository

    private val binder = MusicBinder()
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private var index: Int = -1
    private var tickPosted = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            val mp = mediaPlayer
            if (mp != null && mp.duration > 0) {
                val pos = mp.currentPosition
                val dur = mp.duration
                playbackStateHolder.update { st ->
                    st.copy(
                        positionMs = pos,
                        durationMs = dur,
                        isPlaying = mp.isPlaying,
                        hasActivePlayer = true
                    )
                }
                if (mp.isPlaying) {
                    refreshNotification()
                }
            }
            handler.postDelayed(this, 1000L)
        }
    }

    /** System / MediaStyle routes transport controls here when a session token is attached. */
    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            handler.post { resumeInternal() }
        }

        override fun onPause() {
            handler.post { pauseInternal() }
        }

        override fun onSkipToNext() {
            handler.post { nextInternal() }
        }

        override fun onSkipToPrevious() {
            handler.post { previousInternal() }
        }
    }

    inner class MusicBinder : Binder() {
        fun playSong(song: Song) = playSongInternal(song)
        fun pause() = pauseInternal()
        fun resume() = resumeInternal()
        fun next() = nextInternal()
        fun previous() = previousInternal()
        fun clear() = clearInternal()
        fun seekTo(positionMs: Int) = seekToInternal(positionMs)
    }

    private fun ensureMediaSession(): MediaSessionCompat {
        if (mediaSession == null) {
            mediaSession = MediaSessionCompat(this, "MusicSession").apply {
                setFlags(
                    MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                )
                setCallback(mediaSessionCallback)
                isActive = true
            }
        }
        return mediaSession!!
    }

    override fun onCreate() {
        super.onCreate()
        ensureMediaSession()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_STICKY

        readReceiverAction(intent)?.let {
            handleAction(it)
            return START_STICKY
        }

        if (intent.hasExtra(Constants.EXTRA_SEEK_POSITION_MS)) {
            seekToInternal(intent.getIntExtra(Constants.EXTRA_SEEK_POSITION_MS, 0))
            return START_STICKY
        }

        @Suppress("DEPRECATION")
        val startSong = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Constants.EXTRA_START_SONG, Song::class.java)
        } else {
            intent.getParcelableExtra(Constants.EXTRA_START_SONG)
        }
        if (startSong != null) {
            playSongInternal(startSong)
        }
        return START_STICKY
    }

    private fun readReceiverAction(intent: Intent): Action? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(Constants.RECEIVER_ACTION_MUSIC, Action::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(Constants.RECEIVER_ACTION_MUSIC) as? Action
        }
    }

    private fun handleAction(action: Action) {
        when (action) {
            Action.ACTION_PAUSE -> pauseInternal()
            Action.ACTION_RESUME -> resumeInternal()
            Action.ACTION_NEXT -> nextInternal()
            Action.ACTION_PREVIOUS -> previousInternal()
            Action.ACTION_CLEAR -> clearInternal()
            Action.ACTION_START -> resumeInternal()
            else -> { /* no-op */ }
        }
    }

    private fun startProgressTicker() {
        if (tickPosted) return
        tickPosted = true
        handler.post(tickRunnable)
    }

    private fun stopProgressTicker() {
        tickPosted = false
        handler.removeCallbacks(tickRunnable)
    }

    fun playSongInternal(song: Song) {
        ensureMediaSession()
        stopProgressTicker()
        mediaPlayer?.release()
        mediaPlayer = null

        index = songRepository.indexOf(song).let { if (it < 0) 0 else it }
        val resolved = songRepository.getSong(index)

        mediaPlayer = MediaPlayer.create(this, resolved.sing)?.apply {
            setOnCompletionListener { onTrackCompleted() }
            isLooping = prefs.getTypeRepeat() == Repeat.REPEAT_ONE
            start()
        } ?: return

        val mp = mediaPlayer!!
        val duration = mp.duration.coerceAtLeast(0)
        playbackStateHolder.update {
            PlaybackUiState(
                currentSong = resolved,
                queueIndex = index,
                isPlaying = true,
                positionMs = 0,
                durationMs = duration,
                hasActivePlayer = true
            )
        }
        updateMediaSessionPlaybackState()
        startForeground(1, buildNotification(resolved).build())
        startProgressTicker()
    }

    private fun onTrackCompleted() {
        if (mediaPlayer?.isLooping == true) return
        if (index < songRepository.lastIndex()) {
            index++
            playSongInternal(songRepository.getSong(index))
        } else if (prefs.getTypeRepeat() == Repeat.REPEAT_ALL) {
            index = 0
            playSongInternal(songRepository.getSong(0))
        } else {
            mediaPlayer?.seekTo(0)
            pauseInternal()
            playbackStateHolder.update { it.copy(positionMs = 0) }
        }
    }

    private fun pauseInternal() {
        mediaPlayer?.pause()
        playbackStateHolder.update { it.copy(isPlaying = false) }
        updateMediaSessionPlaybackState()
        mediaPlayer?.let { refreshNotification() }
    }

    private fun resumeInternal() {
        val mp = mediaPlayer ?: return
        if (!mp.isPlaying) {
            mp.start()
        }
        playbackStateHolder.update { it.copy(isPlaying = true) }
        updateMediaSessionPlaybackState()
        refreshNotification()
        startProgressTicker()
    }

    private fun nextInternal() {
        if (index < 0) return
        if (index < songRepository.lastIndex()) {
            index++
        } else if (prefs.getTypeRepeat() == Repeat.REPEAT_ALL) {
            index = 0
        } else {
            cancelNextAtEnd()
            return
        }
        playSongInternal(songRepository.getSong(index))
    }

    private fun previousInternal() {
        if (index < 0) return
        if (index > 0) {
            index--
        } else {
            index = songRepository.lastIndex()
        }
        playSongInternal(songRepository.getSong(index))
    }

    private fun cancelNextAtEnd() {
        mediaPlayer?.seekTo(0)
        pauseInternal()
        playbackStateHolder.update { it.copy(positionMs = 0) }
    }

    private fun seekToInternal(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        playbackStateHolder.update { it.copy(positionMs = positionMs) }
        refreshNotification()
    }

    private fun clearInternal() {
        stopProgressTicker()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        index = -1
        playbackStateHolder.reset()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun updateMediaSessionPlaybackState() {
        val playing = mediaPlayer?.isPlaying == true
        val pos = mediaPlayer?.currentPosition?.toLong() ?: 0L
        val speed = if (playing) 1f else 0f
        val state = if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val session = ensureMediaSession()
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
                )
                .setState(state, pos, speed)
                .build()
        )
    }

    @SuppressLint("ForegroundServiceType")
    private fun buildNotification(song: Song): NotificationCompat.Builder {
        val bitmap = BitmapFactory.decodeResource(resources, song.avatar)
        val session = ensureMediaSession()

        val builder = NotificationCompat.Builder(this, MyApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.music)
            .setSubText("Linh Nguyen")
            .setContentTitle(song.title)
            .setContentText("Ca sĩ: ${song.nameSinger}")
            .setLargeIcon(bitmap)
            .setOnlyAlertOnce(true)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(session.sessionToken)
            )

        val mp = mediaPlayer
        val duration = mp?.duration?.takeIf { it > 0 } ?: 0
        val position = mp?.currentPosition?.takeIf { mp.duration > 0 } ?: 0

        if (mp != null && mp.isPlaying) {
            builder
                .addAction(R.drawable.skip_previous, "Previous", pending(Action.ACTION_PREVIOUS))
                .addAction(R.drawable.pause, "Pause", pending(Action.ACTION_PAUSE))
                .addAction(R.drawable.skip_next, "Next", pending(Action.ACTION_NEXT))
                .setProgress(duration, position, false)
        } else {
            builder
                .addAction(R.drawable.skip_previous, "Previous", pending(Action.ACTION_PREVIOUS))
                .addAction(R.drawable.play, "Play", pending(Action.ACTION_RESUME))
                .addAction(R.drawable.skip_next, "Next", pending(Action.ACTION_NEXT))
                .setProgress(duration, position, false)
        }
        return builder
    }

    private fun refreshNotification() {
        val song = playbackStateHolder.state.value.currentSong ?: return
        startForeground(1, buildNotification(song).build())
    }

    private fun pending(action: Action): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            putExtra(Constants.RECEIVER_ACTION_MUSIC, action)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getForegroundService(
            applicationContext,
            action.ordinal,
            intent,
            flags
        )
    }

    override fun onDestroy() {
        stopProgressTicker()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSession?.setCallback(null)
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
