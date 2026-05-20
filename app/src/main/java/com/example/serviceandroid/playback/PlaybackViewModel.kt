package com.example.serviceandroid.playback

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.data.repository.SongRepository
import com.example.serviceandroid.database.repository.FavouriteSongRepository
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _miniPlayerIsFavourite = MutableStateFlow(false)
    val miniPlayerIsFavourite: StateFlow<Boolean> = _miniPlayerIsFavourite.asStateFlow()

    private var lastSongIdForFavouriteCheck: Int? = null

    init {
        viewModelScope.launch {
            playbackState.collect { st ->
                val id = st.currentSong?.idSong
                if (id != lastSongIdForFavouriteCheck) {
                    lastSongIdForFavouriteCheck = id
                    _miniPlayerIsFavourite.value =
                        id?.let { favouriteSongRepository.checkSongById(it) } ?: false
                }
            }
        }
    }

    fun bind(activity: ComponentActivity) = connector.bind(activity)

    fun unbind(activity: ComponentActivity) = connector.unbind(activity)

    fun setPendingOpenFromMiniPlayer() = stateHolder.setPendingOpenFromMiniPlayer(true)

    fun consumePendingOpenFromMiniPlayer(): Boolean =
        stateHolder.consumePendingOpenFromMiniPlayer()

    fun getPlaylist(): ArrayList<Song> = songRepository.getPlaylist()

    fun resolveQueueIndexForSongId(idSong: Int): Int {
        val idx = songRepository.getPlaylist().indexOfFirst { it.idSong == idSong }
        return if (idx < 0) 0 else idx
    }

    fun playSong(context: Context, song: Song) = connector.playSong(context, song)

    fun playSongAtIndex(context: Context, index: Int) {
        val safe = index.coerceIn(0, songRepository.lastIndex())
        connector.playSong(context, songRepository.getSong(safe))
    }

    fun playFirstSong(context: Context) = playSongAtIndex(context, 0)

    fun pause(context: Context) = connector.pause(context)

    fun resume(context: Context) = connector.resume(context)

    fun next(context: Context) = connector.next(context)

    fun previous(context: Context) = connector.previous(context)

    fun clear(context: Context) = connector.clear(context)

    fun seekTo(context: Context, positionMs: Int) = connector.seekTo(context, positionMs)

    fun syncRepeatMode(context: Context) = connector.syncRepeatMode(context)

    /**
     * Bottom mini-player play button: matches legacy MainActivity logic.
     */
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

    /**
     * Re-read favourite state from DB for the track currently playing (mini player), e.g. after add/remove on full-screen player.
     */
    fun refreshMiniPlayerFavouriteForCurrentSong() {
        viewModelScope.launch {
            val id = stateHolder.state.value.currentSong?.idSong
            _miniPlayerIsFavourite.value =
                id?.let { favouriteSongRepository.checkSongById(it) } ?: false
        }
    }

    /**
     * Mini player favourite: toggle Room favourite for [song]. [onFinished] receives true if song is now favourite, false if removed.
     */
    fun toggleCurrentSongFavourite(song: Song, onFinished: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val nowFavourite = if (favouriteSongRepository.checkSongById(song.idSong)) {
                favouriteSongRepository.deleteSongById(song.idSong)
                false
            } else {
                favouriteSongRepository.insertSong(song, DateUtils.getTimeCurrent())
                true
            }
            _miniPlayerIsFavourite.value = nowFavourite
            onFinished(nowFavourite)
        }
    }
}
