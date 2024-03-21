@file:Suppress("DEPRECATION")

package com.example.serviceandroid.utils

import android.app.Activity
import android.content.Context
import android.graphics.Point

object Convert {
    fun getWidthDevice(context: Context): Int {
        context as Activity
        val display = context.windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size.x
    }

    fun getHeightDevice(context: Context): Int {
        context as Activity
        val display = context.windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size.y
    }
}