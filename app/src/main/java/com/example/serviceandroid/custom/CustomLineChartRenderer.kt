package com.example.serviceandroid.custom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.example.serviceandroid.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.renderer.LineChartRenderer

class CustomLineChartRenderer(
    private val context: Context,
    private val chart: LineChart,
    private val avatarState: ChartAvatarState,
    private val transitionFrom: ChartAvatarState? = null,
    private val animationProgress: () -> Float = { 1f },
) : LineChartRenderer(chart, chart.animator, chart.viewPortHandler) {

    private val interpolator = DecelerateInterpolator()

    override fun drawExtras(c: Canvas) {
        super.drawExtras(c)

        val progress = animationProgress().coerceIn(0f, 1f)
        if (transitionFrom != null && progress < 1f) {
            val eased = interpolator.getInterpolation(progress)
            drawAvatarTransition(c, transitionFrom, avatarState, eased)
        } else {
            drawAvatar(c, avatarState)
        }
    }

    private fun drawAvatarTransition(
        c: Canvas,
        from: ChartAvatarState,
        to: ChartAvatarState,
        progress: Float,
    ) {
        val fromPos = resolveAvatarPosition(from) ?: return
        val toPos = resolveAvatarPosition(to) ?: return

        val centerX = fromPos.centerX + (toPos.centerX - fromPos.centerX) * progress
        val anchorY = fromPos.anchorY + (toPos.anchorY - fromPos.anchorY) * progress

        drawAvatarAt(
            c,
            to.bitmap,
            to.colorRes,
            centerX,
            anchorY,
            "${to.entryIndex + 1}",
        )
    }

    private fun drawAvatar(c: Canvas, state: ChartAvatarState) {
        val pos = resolveAvatarPosition(state) ?: return
        drawAvatarAt(
            c,
            state.bitmap,
            state.colorRes,
            pos.centerX,
            pos.anchorY,
            "${state.entryIndex + 1}",
        )
    }

    private data class AvatarPosition(val centerX: Float, val anchorY: Float)

    private fun resolveAvatarPosition(state: ChartAvatarState): AvatarPosition? {
        val lineData = chart.lineData ?: return null
        if (state.entryIndex < 0 || state.entryIndex >= lineData.dataSetCount) return null
        val dataSet = lineData.getDataSetByIndex(state.entryIndex) ?: return null
        if (state.indexPoint < 0 || state.indexPoint >= dataSet.entryCount) return null
        val entry = dataSet.getEntryForIndex(state.indexPoint) ?: return null

        val transformer = chart.getTransformer(dataSet.axisDependency)
        val pts = floatArrayOf(entry.x, entry.y)
        transformer.pointValuesToPixel(pts)
        return AvatarPosition(pts[0], pts[1])
    }

    private fun drawAvatarAt(
        c: Canvas,
        bitmap: Bitmap,
        colorRes: Int,
        centerX: Float,
        anchorY: Float,
        level: String,
    ) {
        val bitmapCenterCrop = cropBitmapToSquare(bitmap)
        val newWidth = 80
        val newHeight = 80
        val scaledBitmap = bitmapCenterCrop.scale(newWidth, newHeight)
        val strokeBitmap = createRoundedBitmapWithBorder(scaledBitmap, colorRes)

        val left = centerX - strokeBitmap.width / 2f
        val top = anchorY - strokeBitmap.height - 13f
        c.drawBitmap(strokeBitmap, left, top, null)
        drawTextLevel(c, level, left, anchorY - strokeBitmap.height + 67f)
    }

    private fun drawTextLevel(c: Canvas, level: String, x: Float, y: Float) {
        val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = Color.WHITE
        paint.textSize = 49f
        c.drawText(level, x - paint.measureText(level) / 4, y + 3, paint)

        paint.style = Paint.Style.FILL
        paint.color = context.getColor(R.color.black_1)
        c.drawText(level, x - paint.measureText(level) / 4, y + 3, paint)
    }

    private fun cropBitmapToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        return when {
            width > height -> {
                val xOffset = (width - height) / 2
                Bitmap.createBitmap(bitmap, xOffset, 0, height, height)
            }
            width < height -> {
                val yOffset = (height - width) / 2
                Bitmap.createBitmap(bitmap, 0, yOffset, width, width)
            }
            else -> bitmap
        }
    }

    private fun createRoundedBitmapWithBorder(bitmap: Bitmap, colorStroke: Int): Bitmap {
        val cornerRadius = 8f
        val borderWidth = 4f
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        val outputBitmap = createBitmap(
            bitmapWidth + borderWidth.toInt() * 2,
            bitmapHeight + borderWidth.toInt() * 2,
        )
        val canvas = Canvas(outputBitmap)

        val borderPaint = Paint().apply {
            color = ContextCompat.getColor(context, colorStroke)
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            isAntiAlias = true
        }

        val bitmapPaint = Paint().apply {
            isAntiAlias = true
        }

        val rectF = RectF(
            borderWidth,
            borderWidth,
            canvas.width.toFloat() - borderWidth,
            canvas.height.toFloat() - borderWidth,
        )
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, bitmapPaint)

        val path = Path().apply {
            addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CCW)
        }
        canvas.clipPath(path)
        canvas.drawBitmap(bitmap, borderWidth, borderWidth, bitmapPaint)
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)

        return outputBitmap
    }
}
