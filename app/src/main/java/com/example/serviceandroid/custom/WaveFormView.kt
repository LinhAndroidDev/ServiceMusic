package com.example.serviceandroid.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class WaveFormView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): View(context, attrs, defStyleAttr) {
    private var audioData: ShortArray? = null
    private val paint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 2f
        isAntiAlias = true
    }

    fun setAudioData(data: ShortArray) {
        audioData = data
        invalidate()
    }

    fun setOffset(offset: Float) {
        this.setOffset(offset)
        invalidate()
    }


    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        audioData?.let { data ->
            val mid = height / 2
            val max = data.maxOrNull()?.toFloat() ?: 1f
            val scale = mid / max

            for (i in data.indices) {
                val x = i.toFloat() / data.size * width
                val y = mid - data[i] * scale
                canvas.drawLine(x, mid.toFloat(), x, y, paint)
            }
        }
    }
}