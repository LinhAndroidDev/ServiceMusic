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

    private val _sleepTimer = MutableStateFlow(SleepTimerState.idle())
    val sleepTimer: StateFlow<SleepTimerState> = _sleepTimer.asStateFlow()

    private val openFromMiniPlayerPending = AtomicBoolean(false)

    fun update(transform: (PlaybackUiState) -> PlaybackUiState) {
        _state.update(transform)
    }

    fun updateSleepTimer(state: SleepTimerState) {
        _sleepTimer.value = state
    }

    fun resetSleepTimer() {
        _sleepTimer.value = SleepTimerState.idle()
    }

    fun reset() {
        _state.value = PlaybackUiState.idle()
        resetSleepTimer()
    }

    fun setPendingOpenFromMiniPlayer(pending: Boolean) {
        openFromMiniPlayerPending.set(pending)
    }

    fun consumePendingOpenFromMiniPlayer(): Boolean = openFromMiniPlayerPending.getAndSet(false)
}
