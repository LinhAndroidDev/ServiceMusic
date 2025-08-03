package com.example.serviceandroid.utils

import com.example.serviceandroid.database.repository.ArrangeMusic
import com.example.serviceandroid.model.Repeat

interface SharePreferenceRepository {
    companion object {
        const val TYPE_ARRANGEMENT = "TYPE_ARRANGEMENT"
        const val TYPE_REPEAT = "TYPE_REPEAT"
    }

    fun saveTypeArrangement(type: Int)
    fun getTypeArrangement(): ArrangeMusic
    fun saveTypeRepeat(type: Repeat)
    fun getTypeRepeat(): Repeat
}