package com.example.serviceandroid.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    const val TIME = "yyyy/MM/dd HH:mm:ss"
    private const val TIME_CURRENT = "dd/MM/yyyy - HH"

    fun getTimeCurrent(): String {
        val date = SimpleDateFormat(TIME, Locale.getDefault())
        return date.format(Date())
    }

    fun getTimeWithHourCurrent(): String {
        val dateFormat = SimpleDateFormat(TIME_CURRENT, Locale.getDefault())
        val currentDate = Date()
        return dateFormat.format(currentDate) + ":00"
    }

    fun getLast12Hours(): List<Int> {
        val hoursList = mutableListOf<Int>()
        val calendar = Calendar.getInstance()

        // Lấy giờ hiện tại
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        // Bắt đầu từ giờ hiện tại và lùi 12 giờ
        for (i in 0 until 12) {
            calendar.set(Calendar.HOUR_OF_DAY, currentHour)
            calendar.add(Calendar.HOUR_OF_DAY, -i)
            hoursList.add(calendar.get(Calendar.HOUR_OF_DAY))
        }

        // Đảo ngược danh sách để giờ sắp xếp từ nhỏ đến lớn
        return hoursList.reversed()
    }
}