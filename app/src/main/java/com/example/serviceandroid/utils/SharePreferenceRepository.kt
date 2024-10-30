package com.example.serviceandroid.utils

import com.example.serviceandroid.database.repository.ArrangeMusic

interface SharePreferenceRepository {
    companion object {
        const val TYPE_ARRANGEMENT = "TYPE_ARRANGEMENT"
    }

    fun saveTypeArrangement(type: Int)
    fun getTypeArrangement(): ArrangeMusic
}