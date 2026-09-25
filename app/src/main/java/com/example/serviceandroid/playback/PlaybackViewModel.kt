package com.example.serviceandroid.playback

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.data.repository.SongRepository
import com.example.serviceandroid.database.repository.FavouriteSongRepository
import com.example.serviceandroid.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val stateHolder: PlaybackStateHolder,
    private val connector: MusicServiceConnector,
    private val favouriteSongRepository: FavouriteSongRepository,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackUiState> = stateHolder.state
    val sleepTimerState: StateFlow<SleepTimerState> = stateHolder.sleepTimer

    private val _miniPlayerIsFavourite = MutableStateFlow(false)
    val miniPlayerIsFavourite: StateFlow<Boolean> = _miniPlayerIsFavourite.asStateFlow()

    private val _playlistLoading = MutableStateFlow(false)
    val playlistLoading: StateFlow<Boolean> = _playlistLoading.asStateFlow()

    init {
        refreshPlaylist()
        viewModelScope.launch {
            combine(
                playbackState,
                favouriteSongRepository.observeAllRecords(),
            ) { state, favourites ->
                val songId = state.currentSong?.id
                songId != null && favourites.any { it.song.id == songId }
            }.collect { favourite ->
                _miniPlayerIsFavourite.value = favourite
            }
        }
    }

    fun refreshPlaylist() {
        viewModelScope.launch {
            _playlistLoading.value = true
            songRepository.refreshPlaylist()
            _playlistLoading.value = false
        }
    }

    fun bind(activity: ComponentActivity) = connector.bind(activity)

    fun unbind(activity: ComponentActivity) = connector.unbind(activity)

    fun setPendingOpenFromMiniPlayer() = stateHolder.setPendingOpenFromMiniPlayer(true)

    fun consumePendingOpenFromMiniPlayer(): Boolean =
        stateHolder.consumePendingOpenFromMiniPlayer()

    fun getPlaylist(): List<Song> = songRepository.getPlaylist()

    fun setPlaybackQueue(songs: List<Song>) = songRepository.setPlaybackQueue(songs)

    fun playFromVisibleList(context: Context, songs: List<Song>, songId: String): Boolean {
        val song = songs.find { it.id == songId } ?: return false
        setPlaybackQueue(songs)
        playSong(context, song)
        return true
    }

    fun resolveQueueIndexForSongId(songId: String): Int {
        if (songId.isBlank()) return -1
        return songRepository.ensureQueueForSongId(songId)
    }

    fun playSong(context: Context, song: Song) = connector.playSong(context, song)

    fun playSongAtIndex(context: Context, index: Int) {
        if (!songRepository.isLoaded()) return
        val last = songRepository.lastIndex()
        if (last < 0) return
        val safe = index.coerceIn(0, last)
        connector.playSong(context, songRepository.getSong(safe))
    }

    fun playFirstSong(context: Context) = playSongAtIndex(context, 0)

    fun setSleepTimer(
        context: Context,
        option: SleepTimerOption,
        durationMs: Long? = null,
    ) = connector.setSleepTimer(context, option, durationMs)

    fun cancelSleepTimer(context: Context) = connector.cancelSleepTimer(context)

    fun pause(context: Context) = connector.pause(context)

    fun resume(context: Context) = connector.resume(context)

    fun next(context: Context) = connector.next(context)

    fun previous(context: Context) = connector.previous(context)

    fun clear(context: Context) = connector.clear(context)

    fun seekTo(context: Context, positionMs: Int) = connector.seekTo(context, positionMs)

    fun syncRepeatMode(context: Context) = connector.syncRepeatMode(context)

    fun toggleShuffle(context: Context) = connector.toggleShuffle(context)

    fun toggleBottomPlayPause(context: Context, progress: Int, max: Int, isPlaying: Boolean) {
        when {
            max in 1..progress && !isPlaying -> {
                seekTo(context, 0)
                resume(context)
            }
            isPlaying -> pause(context)
            else -> resume(context)
        }
    }

}
