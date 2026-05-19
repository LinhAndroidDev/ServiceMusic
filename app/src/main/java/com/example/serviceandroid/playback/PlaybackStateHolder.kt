package com.example.serviceandroid.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackStateHolder @Inject constructor() {

    private val _state = MutableStateFlow(PlaybackUiState.idle())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val openFromMiniPlayerPending = AtomicBoolean(false)

    fun update(transform: (PlaybackUiState) -> PlaybackUiState) {
        _state.update(transform)
    }

    fun reset() {
        _state.value = PlaybackUiState.idle()
    }

    fun setPendingOpenFromMiniPlayer(pending: Boolean) {
        openFromMiniPlayerPending.set(pending)
    }

    fun consumePendingOpenFromMiniPlayer(): Boolean = openFromMiniPlayerPending.getAndSet(false)
}
