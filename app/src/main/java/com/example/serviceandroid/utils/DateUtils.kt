package com.example.serviceandroid.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    const val TIME = "yyyy/MM/dd HH:mm:ss"
    fun getTimeCurrent(): String {
        val date = SimpleDateFormat(TIME, Locale.getDefault())
        return date.format(Date())
    }
}