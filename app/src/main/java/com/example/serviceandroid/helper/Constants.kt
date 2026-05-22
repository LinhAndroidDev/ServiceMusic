package com.example.serviceandroid.helper

object Constants {
    /** Same string as legacy [com.example.serviceandroid.MainActivity.MESSAGE_MAIN] for intents. */
    const val EXTRA_START_SONG = "MESSAGE_MAIN"
    const val EXTRA_SEEK_POSITION_MS = "EXTRA_SEEK_POSITION_MS"

    const val OBJECT_SONG = "OBJECT_SONG"
    const val STATUS_PLAYING = "STATUS_PLAYING"
    const val ACTION_MUSIC = "ACTION_MUSIC"
    const val SEND_DATA_TO_ACTIVITY = "SEND_DATA_TO_ACTIVITY"
    const val RECEIVER_ACTION_MUSIC = "RECEIVER_ACTION_MUSIC"

    /** Mở [com.example.serviceandroid.fragment.music.FragmentMusic] khi user tap vùng nội dung notification (không phải nút điều khiển). */
    const val EXTRA_OPEN_PLAYER_FROM_NOTIFICATION = "EXTRA_OPEN_PLAYER_FROM_NOTIFICATION"
    const val EXTRA_NOTIFICATION_TARGET_SONG_ID = "EXTRA_NOTIFICATION_TARGET_SONG_ID"
    const val MINUTES = "mm:ss"
    const val TIME_ROTATE = 25000L
}