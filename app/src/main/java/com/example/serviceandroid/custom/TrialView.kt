package com.example.serviceandroid.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.example.serviceandroid.R

class TrialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var paint: Paint? = null

    init {
        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.TrialView, 0, 0)
        val colorTrial = array.getColor(R.styleable.TrialView_color_trial, context.getColor(R.color.purple_1))
        paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTrial
            style = Paint.Style.FILL
        }
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val path = Path()
        path.moveTo(width / 2f, 0f)
        path.lineTo(0f, height.toFloat())
        path.lineTo(width.toFloat(), height.toFloat())
        path.close()

        paint?.let {
            canvas.drawPath(path, it)
        }
    }
}