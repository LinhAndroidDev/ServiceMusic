package com.example.serviceandroid.playback

import com.example.serviceandroid.model.Song

/**
 * UI-facing playback snapshot (updated from [MusicService] on the main thread).
 */
data class PlaybackUiState(
    val currentSong: Song? = null,
    val queueIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val hasActivePlayer: Boolean = false,
    /** Bumped on each explicit seek so UI can bypass progress throttling. */
    val seekSequence: Long = 0L,
) {
    companion object {
        fun idle() = PlaybackUiState()
    }
}
