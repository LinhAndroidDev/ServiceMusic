package com.example.serviceandroid.fragment.favourite_song

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviceandroid.database.repository.FavouriteSongRepository
import com.example.serviceandroid.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FragmentFavouriteSongViewModel @Inject constructor(private val repository: FavouriteSongRepository) : ViewModel() {
    val songs = MutableStateFlow<MutableList<Song>?>(null)
    suspend fun getAll() = viewModelScope.launch {
        songs.value = repository.getAll()
    }

    fun insertSong(song: Song) = viewModelScope.launch {
        repository.insertSong(song)
    }

    fun deleteSongById(id: Int) = viewModelScope.launch {
        repository.deleteSongById(id)
        getAll()
    }
}