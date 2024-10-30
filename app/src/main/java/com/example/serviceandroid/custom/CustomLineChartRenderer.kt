package com.example.serviceandroid.custom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.example.serviceandroid.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.renderer.LineChartRenderer

class CustomLineChartRenderer(
    private val context: Context,
    private val chart: LineChart,
    private val entryIndex: Int,
    private val indexPoint: Int,
    private val color: Int,
    private val bitmap: Bitmap
) : LineChartRenderer(chart, chart.animator, chart.viewPortHandler) {

    override fun drawExtras(c: Canvas) {
        super.drawExtras(c)

        // Tìm vị trí của Entry thứ indexPoint (nếu có)
        val dataSet = chart.lineData.getDataSetByIndex(entryIndex)
        val entry = dataSet.getEntryForIndex(indexPoint) // Điểm thứ indexPoint

        if (entry != null) {
            // Sử dụng Transformer để lấy tọa độ pixel của điểm
            val transformer = chart.getTransformer(dataSet.axisDependency)
            val pts = floatArrayOf(entry.x, entry.y)
            transformer.pointValuesToPixel(pts)

            val bitmapCenterCrop = cropBitmapToSquare(bitmap)

            // Tạo một Bitmap đã được thu nhỏ
            val newWidth = 80  // Đặt kích thước mong muốn cho chiều rộng
            val newHeight = 80 // Đặt kích thước mong muốn cho chiều cao
            val scaledBitmap = Bitmap.createScaledBitmap(bitmapCenterCrop, newWidth, newHeight, true)
            val strokeBitmap = createRoundedBitmapWithBorder(scaledBitmap, color)

            // Vẽ ảnh đã thu nhỏ tại vị trí indexPoint
            c.drawBitmap(strokeBitmap, pts[0] - (strokeBitmap.width / 2), pts[1] - (strokeBitmap.height) - 13, null)
            drawTextLevel(c, "${entryIndex + 1}", (pts[0] - (strokeBitmap.width / 2)), pts[1] - (strokeBitmap.height) + 67)
        }
    }

    private fun drawTextLevel(c: Canvas, level: String, x: Float, y: Float) {

        // Vẽ viền (stroke)
        val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f // Độ dày của viền
        paint.setTypeface(Typeface.DEFAULT_BOLD)
        paint.color = Color.WHITE
        paint.textSize = 49f
        c.drawText(level, x - paint.measureText(level)/4, y + 3, paint)

        // Vẽ text màu bên trong
        paint.style = Paint.Style.FILL
        paint.color = context.getColor(R.color.black_1)
        c.drawText(level, x - paint.measureText(level)/4, y + 3, paint)
    }

    private fun cropBitmapToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        return when {
            width > height -> {
                // Cắt từ trung tâm theo chiều rộng
                val xOffset = (width - height) / 2
                Bitmap.createBitmap(bitmap, xOffset, 0, height, height)
            }
            width < height -> {
                // Cắt từ trung tâm theo chiều cao
                val yOffset = (height - width) / 2
                Bitmap.createBitmap(bitmap, 0, yOffset, width, width)
            }
            else -> {
                // Nếu đã là hình vuông thì giữ nguyên
                bitmap
            }
        }
    }

    private fun createRoundedBitmapWithBorder(bitmap: Bitmap, colorStroke: Int): Bitmap {
        // Đặt kích thước và các thông số
        val cornerRadius = 8f  // Bo góc với 8dp
        val borderWidth = 4f  // Viền màu xanh với độ dày 4dp
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        // Tạo Bitmap mới để vẽ lên đó
        val outputBitmap = Bitmap.createBitmap(
            bitmapWidth + borderWidth.toInt() * 2,
            bitmapHeight + borderWidth.toInt() * 2,
            Bitmap.Config.ARGB_8888
        )

        // Tạo Canvas để vẽ lên Bitmap mới
        val canvas = Canvas(outputBitmap)

        // Tạo Paint để vẽ viền
        val borderPaint = Paint().apply {
            color = ContextCompat.getColor(context, colorStroke)
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            isAntiAlias = true
        }

        // Tạo Paint để vẽ Bitmap
        val bitmapPaint = Paint().apply {
            isAntiAlias = true
        }

        // Vẽ hình chữ nhật với các góc bo tròn
        val rectF = RectF(
            borderWidth,
            borderWidth,
            canvas.width.toFloat() - borderWidth,
            canvas.height.toFloat() - borderWidth
        )
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, bitmapPaint)

        // Tạo path bo góc
        val path = Path().apply {
            addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CCW)
        }
        canvas.clipPath(path)

        // Vẽ Bitmap bên trong các góc bo tròn
        canvas.drawBitmap(bitmap, borderWidth, borderWidth, bitmapPaint)

        // Vẽ viền xung quanh Bitmap
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, borderPaint)

        return outputBitmap
    }
}