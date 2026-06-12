package com.example.serviceandroid.utils

import android.content.Context
import com.example.serviceandroid.database.repository.ArrangeMusic
import com.example.serviceandroid.model.Repeat

class SharePreferenceRepositoryImpl(private val ctx: Context) : SharePreferenceRepository,
    PreferenceUtil(ctx) {

    private val prefs by lazy { defaultPref() }

    override fun saveTypeArrangement(type: Int) {
        prefs[SharePreferenceRepository.TYPE_ARRANGEMENT] = type
    }

    override fun getTypeArrangement(): ArrangeMusic {
        return ArrangeMusic.entries[prefs[SharePreferenceRepository.TYPE_ARRANGEMENT] ?: 0]
    }

    override fun saveTypeRepeat(type: Repeat) {
        prefs[SharePreferenceRepository.TYPE_REPEAT] = type.value
    }

    override fun getTypeRepeat(): Repeat {
        return Repeat.of(prefs[SharePreferenceRepository.TYPE_REPEAT] ?: 0)
    }
}