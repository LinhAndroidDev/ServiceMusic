package com.example.serviceandroid.custom

import com.github.mikephil.charting.formatter.ValueFormatter

class CustomXAxisFormatter(private val labels: List<Int>) : ValueFormatter() {
    override fun getFormattedValue(value: Float): String {
        val index = value.toInt()
        return if (index in labels.indices) {
            if(labels[index] < 10) {
                "0" + labels[index].toString()
            } else {
                labels[index].toString()
            }
        } else ""
    }
}