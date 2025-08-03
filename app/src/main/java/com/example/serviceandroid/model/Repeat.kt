package com.example.serviceandroid.model

import com.example.serviceandroid.R

enum class Repeat(val value: Int) {
    NOT_REPEAT(R.drawable.ic_not_repeat),
    REPEAT_ALL(R.drawable.ic_repeat_all),
    REPEAT_ONE(R.drawable.ic_repeat_one);

    companion object {
        fun of(value: Int): Repeat {
            return entries.find { it.value == value } ?: NOT_REPEAT
        }
    }
}