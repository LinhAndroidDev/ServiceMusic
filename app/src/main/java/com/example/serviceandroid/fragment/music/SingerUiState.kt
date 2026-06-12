package com.example.serviceandroid.fragment.music

import com.example.serviceandroid.model.Singer

data class SingerUiState(
    val isLoading: Boolean = false,
    val songId: String = "",
    val singers: List<Singer> = emptyList(),
    val selectedIndex: Int = 0,
) {
    val selectedSinger: Singer?
        get() = singers.getOrNull(selectedIndex)
}
