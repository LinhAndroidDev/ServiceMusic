package com.example.serviceandroid.fragment.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.data.firestore.FirestoreMusicRepository
import com.example.serviceandroid.database.repository.FavouriteSongRepository
import com.example.serviceandroid.model.Repeat
import com.example.serviceandroid.model.Singer
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.model.toDomainSinger
import com.example.serviceandroid.utils.SharePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FragmentMusicViewModel @Inject constructor(
    private val repository: FavouriteSongRepository,
    private val shared: SharePreferenceRepository,
    private val firestoreMusicRepository: FirestoreMusicRepository,
) : ViewModel() {
    private val _isFavourite: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isFavourite get() = _isFavourite

    private val _singerUiState = MutableStateFlow(SingerUiState())
    val singerUiState: StateFlow<SingerUiState> = _singerUiState.asStateFlow()

    fun insertSong(song: Song, timeCreate: String, onCallBackInsertSong: () -> Unit) =
        viewModelScope.launch {
            repository.insertSong(song, timeCreate)
            _isFavourite.value = repository.checkSongById(song.id)
            onCallBackInsertSong.invoke()
        }

    fun deleteSongById(id: String, onCallBackDeleteSong: () -> Unit) = viewModelScope.launch {
        repository.deleteSongById(id)
        _isFavourite.value = repository.checkSongById(id)
        onCallBackDeleteSong.invoke()
    }

    fun checkSongById(id: String) = viewModelScope.launch {
        _isFavourite.value = repository.checkSongById(id)
    }

    fun loadSingersForSong(songId: String, fallbackSingerName: String) {
        if (songId.isBlank()) return
        viewModelScope.launch {
            _singerUiState.value = SingerUiState(isLoading = true, songId = songId)
            val singers = loadSingersFromFirestore(songId, fallbackSingerName)
            if (_singerUiState.value.songId != songId) return@launch
            _singerUiState.value = SingerUiState(
                isLoading = false,
                songId = songId,
                singers = singers,
                selectedIndex = 0,
            )
        }
    }

    fun selectSingerTab(index: Int) {
        val state = _singerUiState.value
        if (index !in state.singers.indices || index == state.selectedIndex) return
        _singerUiState.value = state.copy(selectedIndex = index)
    }

    private suspend fun loadSingersFromFirestore(
        songId: String,
        fallbackSingerName: String,
    ): List<Singer> {
        val firestoreSong = firestoreMusicRepository.getSong(songId)
        val singerIds = firestoreSong?.displaySingerIds.orEmpty().filter { it.isNotBlank() }
        if (singerIds.isEmpty()) {
            val name = firestoreSong?.artistText?.takeIf { it.isNotBlank() }
                ?: fallbackSingerName.takeIf { it.isNotBlank() }
                ?: return emptyList()
            return listOf(
                Singer(
                    id = "",
                    name = name,
                    avatarUrl = "",
                    description = "",
                ),
            )
        }
        return coroutineScope {
            singerIds.map { singerId ->
                async {
                    firestoreMusicRepository.getSinger(singerId)?.toDomainSinger()
                }
            }.awaitAll().filterNotNull()
        }
    }

    fun getTypeRepeat() = shared.getTypeRepeat()

    fun saveTypeRepeat(type: Repeat) {
        shared.saveTypeRepeat(type)
    }
}
