package com.example.serviceandroid.fragment.favourite_song

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.database.SongEntity
import com.example.serviceandroid.database.repository.FavouriteSongRepository
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.SharePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FragmentFavouriteSongViewModel @Inject constructor(
    private val repository: FavouriteSongRepository,
    private val shared: SharePreferenceRepository,
) : ViewModel() {

    private val songsMutable = MutableStateFlow<MutableList<Song>?>(null)
    val songs: StateFlow<MutableList<Song>?> = songsMutable.asStateFlow()

    private val favouriteCountMutable = MutableStateFlow(0)
    val favouriteCount: StateFlow<Int> = favouriteCountMutable.asStateFlow()

    private var lastEntities: List<SongEntity> = emptyList()

    init {
        viewModelScope.launch {
            repository.observeFavouriteCount().collect { count ->
                favouriteCountMutable.value = count
            }
        }
        viewModelScope.launch {
            repository.observeAllEntities().collect { entities ->
                lastEntities = entities
                songsMutable.value =
                    repository.entitiesToSortedSongs(entities, shared.getTypeArrangement())
            }
        }
    }

    /** Khi chỉ đổi kiểu sắp xếp (không đổi dữ liệu Room), áp dụng lại sort lên snapshot hiện tại. */
    fun applyCurrentArrangement() {
        viewModelScope.launch {
            songsMutable.value =
                repository.entitiesToSortedSongs(lastEntities, shared.getTypeArrangement())
        }
    }

    fun deleteSongById(id: String, callBackDeleteSong: () -> Unit) = viewModelScope.launch {
        repository.deleteSongById(id)
        callBackDeleteSong.invoke()
    }

    fun getTypeArrangement() = shared.getTypeArrangement()
}
