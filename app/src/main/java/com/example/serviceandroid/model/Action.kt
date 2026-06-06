package com.example.serviceandroid.model

enum class Action {
    ACTION_PAUSE,
    ACTION_RESUME,
    ACTION_CLEAR,
    ACTION_START,
    ACTION_NEXT,
    ACTION_PREVIOUS,
    ACTION_FINISH,
    /** UI changed repeat mode; service refreshes ExoPlayer repeat mode from prefs. */
    ACTION_SYNC_REPEAT,
}