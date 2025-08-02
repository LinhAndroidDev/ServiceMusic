package com.example.serviceandroid

import android.media.MediaPlayer

interface PlayCallback {
    var isPlaying: Boolean
    var isRepeat: Boolean
    var isFinish: Boolean
    var indexSong: Int
    var mediaPlayer: MediaPlayer?
}