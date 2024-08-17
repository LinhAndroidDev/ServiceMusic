package com.example.serviceandroid.fragment.favourite_song

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.database.repository.FavouriteSongRepository
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.SharePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FragmentFavouriteSongViewModel @Inject constructor(private val repository: FavouriteSongRepository) : ViewModel() {
    @Inject
    lateinit var shared: SharePreferenceRepository

    val songs = MutableStateFlow<MutableList<Song>?>(null)
    fun getAll() = viewModelScope.launch {
        songs.value = repository.getAll(shared.getTypeArrangement())
    }

    fun deleteSongById(id: Int, callBackDeleteSong: () -> Unit) = viewModelScope.launch {
        repository.deleteSongById(id)
        getAll()
        callBackDeleteSong.invoke()
    }
}