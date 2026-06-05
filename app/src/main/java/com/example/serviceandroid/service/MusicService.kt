package com.example.serviceandroid.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.util.Log
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.example.serviceandroid.MainActivity
import com.example.serviceandroid.R
import com.example.serviceandroid.data.firestore.FirestoreMusicRepository
import com.example.serviceandroid.data.repository.SongRepository
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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

    @Inject
    lateinit var firestoreMusicRepository: FirestoreMusicRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var prepareGeneration = 0

    private val binder = MusicBinder()
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private var index: Int = -1
    private var tickPosted = false
    private var msSinceNotificationRefresh: Long = 0
    private var snapshotTickCounter: Int = 0
    private var lastSeekPositionMs: Int = -1
    private var lastSeekElapsedMs: Long = 0L

    /** Frequent position updates for UI (lyrics); notification refreshed at [NOTIFICATION_REFRESH_MS]. */
    private val tickIntervalMs = 80L
    private val notificationRefreshMs = 1000L

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!tickPosted) return
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
                    msSinceNotificationRefresh += tickIntervalMs
                    if (msSinceNotificationRefresh >= notificationRefreshMs) {
                        msSinceNotificationRefresh = 0L
                        refreshNotification()
                    }
                }
                snapshotTickCounter++
                if (snapshotTickCounter >= SNAPSHOT_TICKS_INTERVAL) {
                    snapshotTickCounter = 0
                    persistPlaybackSnapshot()
                }
            }
            if (tickPosted) {
                handler.postDelayed(this, tickIntervalMs)
            }
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
        fun syncRepeatFromPrefs() = applyRepeatFromPrefs()
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
        if (intent == null) {
            if (!restorePlaybackFromPersistedState()) {
                stopSelf()
            }
            return START_STICKY
        }

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

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Vuốt app khỏi danh sách gần đây — dừng phát và gỡ notification (user "kill" task).
        clearInternal()
        super.onTaskRemoved(rootIntent)
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
            Action.ACTION_SYNC_REPEAT -> applyRepeatFromPrefs()
            Action.ACTION_FINISH -> { /* reserved */ }
        }
    }

    private fun startProgressTicker() {
        if (tickPosted) return
        msSinceNotificationRefresh = 0L
        snapshotTickCounter = 0
        tickPosted = true
        handler.post(tickRunnable)
    }

    private fun stopProgressTicker() {
        tickPosted = false
        msSinceNotificationRefresh = 0L
        handler.removeCallbacks(tickRunnable)
    }

    private fun applyRepeatFromPrefs() {
        mediaPlayer?.isLooping = prefs.getTypeRepeat() == Repeat.REPEAT_ONE
        refreshNotification()
    }

    fun playSongInternal(song: Song) {
        ensureMediaSession()
        stopProgressTicker()
        releaseMediaPlayer()

        index = songRepository.indexOf(song).let { if (it < 0) 0 else it }
        val resolved = songRepository.getSong(index)
        val estimatedDurationMs = (resolved.durationSec * 1000L).toInt().coerceAtLeast(0)

        playbackStateHolder.update {
            PlaybackUiState(
                currentSong = resolved,
                queueIndex = index,
                isPlaying = false,
                positionMs = 0,
                durationMs = estimatedDurationMs,
                hasActivePlayer = true,
            )
        }
        updateMediaSessionPlaybackState()
        showForegroundWithPlaceholder(resolved)
        startStreaming(resolved, startPositionMs = 0, autoStart = true)
    }

    private fun releaseMediaPlayer() {
        prepareGeneration++
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun startStreaming(resolved: Song, startPositionMs: Int, autoStart: Boolean) {
        if (resolved.audioUrl.isBlank()) {
            Log.e(TAG, "Missing audioUrl for song ${resolved.id}")
            return
        }
        val generation = ++prepareGeneration
        val player = MediaPlayer()
        mediaPlayer = player
        player.setOnCompletionListener {
            if (generation == prepareGeneration) onTrackCompleted()
        }
        player.setOnErrorListener { _, what, extra ->
            Log.e(TAG, "MediaPlayer error what=$what extra=$extra url=${resolved.audioUrl}")
            if (generation == prepareGeneration) {
                playbackStateHolder.update { it.copy(isPlaying = false) }
            }
            true
        }
        player.setOnPreparedListener { mp ->
            if (generation != prepareGeneration) return@setOnPreparedListener
            mp.isLooping = prefs.getTypeRepeat() == Repeat.REPEAT_ONE
            val dur = mp.duration.coerceAtLeast(0)
            val safePos = if (dur > 0) startPositionMs.coerceIn(0, dur) else startPositionMs.coerceAtLeast(0)
            if (safePos > 0) mp.seekTo(safePos)
            if (autoStart) mp.start()
            playbackStateHolder.update {
                PlaybackUiState(
                    currentSong = resolved,
                    queueIndex = index,
                    isPlaying = autoStart,
                    positionMs = safePos,
                    durationMs = dur,
                    hasActivePlayer = true,
                )
            }
            updateMediaSessionPlaybackState()
            refreshNotificationAsync(resolved)
            persistPlaybackSnapshot()
            if (autoStart) {
                serviceScope.launch {
                    runCatching { firestoreMusicRepository.incrementViews(resolved.id) }
                }
                startProgressTicker()
            }
        }
        try {
            player.setDataSource(resolved.audioUrl)
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "setDataSource failed", e)
            if (generation == prepareGeneration) releaseMediaPlayer()
        }
    }

    private fun showForegroundWithPlaceholder(song: Song) {
        startForegroundTyped(buildNotification(song, null).build())
    }

    private fun refreshNotificationAsync(song: Song) {
        serviceScope.launch {
            val bitmap = loadThumbnailBitmap(song.thumbnailUrl)
            if (playbackStateHolder.state.value.currentSong?.id == song.id) {
                startForegroundTyped(buildNotification(song, bitmap).build())
            }
        }
    }

    private suspend fun loadThumbnailBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        runCatching {
            Glide.with(applicationContext)
                .asBitmap()
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .submit()
                .get()
        }.getOrNull()
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
        stopProgressTicker()
        mediaPlayer?.let {
            refreshNotification()
            persistPlaybackSnapshot()
        }
    }

    private fun resumeInternal() {
        if (mediaPlayer == null && !restorePlaybackFromPersistedState()) {
            return
        }
        val mp = mediaPlayer ?: return
        if (!mp.isPlaying) {
            mp.start()
        }
        playbackStateHolder.update { it.copy(isPlaying = true) }
        updateMediaSessionPlaybackState()
        refreshNotification()
        persistPlaybackSnapshot()
        startProgressTicker()
    }

    private fun nextInternal() {
        ensureActiveSessionOrRestore()
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
        ensureActiveSessionOrRestore()
        if (index < 0) return
        if (index > 0) {
            index--
        } else {
            index = songRepository.lastIndex()
        }
        playSongInternal(songRepository.getSong(index))
    }

    private fun ensureActiveSessionOrRestore() {
        if (index < 0 || mediaPlayer == null) {
            restorePlaybackFromPersistedState()
        }
    }

    private fun cancelNextAtEnd() {
        mediaPlayer?.seekTo(0)
        pauseInternal()
        playbackStateHolder.update { it.copy(positionMs = 0) }
    }

    private fun seekToInternal(positionMs: Int) {
        val mp = mediaPlayer ?: return
        val dur = mp.takeIf { it.duration > 0 }?.duration ?: 0
        val safe = if (dur > 0) positionMs.coerceIn(0, dur) else positionMs.coerceAtLeast(0)
        val now = SystemClock.elapsedRealtime()
        if (safe == lastSeekPositionMs && now - lastSeekElapsedMs < SEEK_DEBOUNCE_MS) {
            return
        }
        lastSeekPositionMs = safe
        lastSeekElapsedMs = now
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mp.seekTo(safe.toLong(), MediaPlayer.SEEK_CLOSEST)
            } else {
                @Suppress("DEPRECATION")
                mp.seekTo(safe)
            }
        } catch (e: Exception) {
            Log.e(TAG, "seekTo failed at $safe", e)
            return
        }
        playbackStateHolder.update {
            it.copy(
                positionMs = safe,
                seekSequence = it.seekSequence + 1L,
            )
        }
        updateMediaSessionPlaybackState()
        persistPlaybackSnapshot()
    }

    private fun clearInternal() {
        tearDownServicePlayback()
        stopSelf()
    }

    private fun tearDownServicePlayback() {
        stopProgressTicker()
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        releaseMediaPlayer()
        index = -1
        clearPersistedPlayback()
        playbackStateHolder.reset()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: IllegalStateException) {
        }
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
    private fun buildNotification(song: Song, bitmap: Bitmap?): NotificationCompat.Builder {
        val largeIcon = bitmap ?: BitmapFactory.decodeResource(resources, R.drawable.ic_circle)
        val session = ensureMediaSession()
        val openPlayer = openPlayerContentPendingIntent(song)
        session.setSessionActivity(openPlayer)

        val builder = NotificationCompat.Builder(this, MyApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.music)
            .setSubText("Linh Nguyen")
            .setContentTitle(song.title)
            .setContentText("Ca sĩ: ${song.nameSinger}")
            .setLargeIcon(largeIcon)
            .setContentIntent(openPlayer)
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
        refreshNotificationAsync(song)
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundTyped(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun restorePlaybackFromPersistedState(): Boolean {
        val sp = getSharedPreferences(PREF_RESTORE, Context.MODE_PRIVATE)
        if (!sp.getBoolean(KEY_ACTIVE, false)) {
            return false
        }
        val idx = sp.getInt(KEY_QUEUE_INDEX, -1)
        val posMs = sp.getInt(KEY_POSITION_MS, 0)
        val wasPlaying = sp.getBoolean(KEY_WAS_PLAYING, false)
        if (!songRepository.isLoaded()) {
            runBlocking { songRepository.refreshPlaylist() }
        }
        if (!songRepository.isLoaded() || idx < 0 || idx > songRepository.lastIndex()) {
            clearPersistedPlayback()
            return false
        }
        ensureMediaSession()
        index = idx
        val resolved = songRepository.getSong(index)
        val estimatedDurationMs = (resolved.durationSec * 1000L).toInt().coerceAtLeast(0)
        playbackStateHolder.update {
            PlaybackUiState(
                currentSong = resolved,
                queueIndex = index,
                isPlaying = false,
                positionMs = posMs.coerceAtLeast(0),
                durationMs = estimatedDurationMs,
                hasActivePlayer = true,
            )
        }
        updateMediaSessionPlaybackState()
        showForegroundWithPlaceholder(resolved)
        startStreaming(resolved, startPositionMs = posMs, autoStart = wasPlaying)
        return true
    }

    private fun persistPlaybackSnapshot() {
        val mp = mediaPlayer ?: return
        if (index < 0 || index > songRepository.lastIndex()) return
        val dur = mp.duration
        val pos = if (dur > 0) mp.currentPosition.coerceIn(0, dur) else mp.currentPosition.coerceAtLeast(0)
        getSharedPreferences(PREF_RESTORE, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ACTIVE, true)
            .putInt(KEY_QUEUE_INDEX, index)
            .putInt(KEY_POSITION_MS, pos)
            .putBoolean(KEY_WAS_PLAYING, mp.isPlaying)
            .apply()
    }

    private fun clearPersistedPlayback() {
        getSharedPreferences(PREF_RESTORE, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun openPlayerContentPendingIntent(song: Song): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(Constants.EXTRA_OPEN_PLAYER_FROM_NOTIFICATION, true)
            putExtra(Constants.EXTRA_NOTIFICATION_TARGET_SONG_ID, song.id)
        }
        return PendingIntent.getActivity(
            this,
            REQUEST_CODE_OPEN_PLAYER_FROM_NOTIFICATION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
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
        tearDownServicePlayback()
        mediaSession?.setCallback(null)
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val PREF_RESTORE = "music_playback_restore"
        private const val KEY_ACTIVE = "active"
        private const val KEY_QUEUE_INDEX = "queue_index"
        private const val KEY_POSITION_MS = "position_ms"
        private const val KEY_WAS_PLAYING = "was_playing"
        /** ~2s at 80ms/tick — đủ để khôi phục vị trí sau process bị kill mà không ghi prefs quá dày. */
        private const val SNAPSHOT_TICKS_INTERVAL = 25
        private const val REQUEST_CODE_OPEN_PLAYER_FROM_NOTIFICATION = 3100
        private const val TAG = "MusicService"
        /** Ignore duplicate seeks within this window (stream re-buffer is expensive). */
        private const val SEEK_DEBOUNCE_MS = 200L
    }
}
