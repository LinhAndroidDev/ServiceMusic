package com.example.serviceandroid.playback

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import com.example.serviceandroid.data.repository.SongRepository
import com.example.serviceandroid.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val stateHolder: PlaybackStateHolder,
    private val connector: MusicServiceConnector,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackUiState> = stateHolder.state

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
}
