package com.example.serviceandroid.fragment.zingchart

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var path = Path()
    private var paint = Paint()
    private var canvasBitmap: Bitmap? = null
    private var drawCanvas: Canvas? = null
    private var canvasPaint = Paint(Paint.DITHER_FLAG)
    private var brushSize: Float = 10f
    private var color: Int = Color.BLACK

    init {
        setupDrawing()
    }

    private fun setupDrawing() {
        paint.color = color
        paint.isAntiAlias = true
        paint.strokeWidth = brushSize
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(canvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(canvasBitmap!!, 0f, 0f, canvasPaint)
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(touchX, touchY)
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(touchX, touchY)
            }
            MotionEvent.ACTION_UP -> {
                drawCanvas?.drawPath(path, paint)
                path.reset()
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun setColor(newColor: Int) {
        invalidate()
        color = newColor
        paint.color = color
    }

    fun setBrushSize(newSize: Float) {
        brushSize = newSize
        paint.strokeWidth = brushSize
    }
}