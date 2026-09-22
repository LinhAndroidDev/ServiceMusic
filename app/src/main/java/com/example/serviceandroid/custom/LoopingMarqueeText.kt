package com.example.serviceandroid.custom

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.ceil

@SuppressLint("ResourceType")
class LoopingMarqueeText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val textPaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG)
    private val fadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private var fadeShaderWidth = -1
    private var fadeShaderLeft = false
    private var fadingLeft = false
    private val gapPx = GAP_DP * resources.displayMetrics.density
    private val speedPxPerSec = SPEED_DP_PER_SEC * resources.displayMetrics.density

    private var label = ""
    private var textWidth = 0f
    private var offset = 0f
    private var animator: ValueAnimator? = null
    private var cancelled = false
    private val resumeScroll = Runnable {
        offset = 0f
        invalidate()
        animator?.start()
    }

    init {
        val styled = context.obtainStyledAttributes(
            attrs,
            intArrayOf(
                android.R.attr.text,
                android.R.attr.textColor,
                android.R.attr.textSize,
                android.R.attr.textStyle,
            ),
        )
        val text = styled.getText(0)
        textPaint.color = styled.getColor(1, Color.WHITE)
        textPaint.textSize = styled.getDimension(2, 14f * resources.displayMetrics.scaledDensity)
        textPaint.typeface = Typeface.create(
            Typeface.DEFAULT,
            styled.getInt(3, Typeface.NORMAL),
        )
        styled.recycle()
        if (!text.isNullOrEmpty()) setText(text)
    }

    fun setText(value: CharSequence?) {
        val next = value?.toString().orEmpty()
        if (next == label) {
            restart()
            return
        }
        label = next
        contentDescription = next
        textWidth = textPaint.measureText(label)
        restart()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val textHeight = ceil(textPaint.descent() - textPaint.ascent()).toInt()
        val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            else -> textHeight
        }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        if (label.isEmpty()) return
        val y = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        if (!shouldScroll()) {
            canvas.drawText(label, 0f, y, textPaint)
            return
        }
        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawText(label, -offset, y, textPaint)
        canvas.drawText(label, -offset + textWidth + gapPx, y, textPaint)
        ensureFadeShader(width, fadingLeft)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fadePaint)
        canvas.restoreToCount(layer)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        restart()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        restart()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    private fun ensureFadeShader(viewWidth: Int, fadeLeftEdge: Boolean) {
        if (viewWidth <= 0) return
        if (viewWidth == fadeShaderWidth && fadeLeftEdge == fadeShaderLeft) return
        fadeShaderWidth = viewWidth
        fadeShaderLeft = fadeLeftEdge
        val edge = (FADE_DP * resources.displayMetrics.density).coerceAtMost(viewWidth / 4f)
        val start = edge / viewWidth
        val end = 1f - start
        val leftColor = if (fadeLeftEdge) Color.TRANSPARENT else Color.BLACK
        fadePaint.shader = LinearGradient(
            0f,
            0f,
            viewWidth.toFloat(),
            0f,
            intArrayOf(leftColor, Color.BLACK, Color.BLACK, Color.TRANSPARENT),
            floatArrayOf(0f, start, end, 1f),
            Shader.TileMode.CLAMP,
        )
    }

    private fun shouldScroll(): Boolean = textWidth > width && width > 0

    private fun restart() {
        stop()
        fadingLeft = false
        offset = 0f
        invalidate()
        if (!isAttachedToWindow || !shouldScroll()) return
        val distance = textWidth + gapPx
        val duration = (distance / speedPxPerSec * 1000f).toLong().coerceAtLeast(1L)
        animator = ValueAnimator.ofFloat(0f, distance).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
            addUpdateListener {
                fadingLeft = true
                offset = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (cancelled || animator !== animation) {
                        cancelled = false
                        return
                    }
                    fadingLeft = false
                    offset = distance
                    invalidate()
                    postDelayed(resumeScroll, LOOP_PAUSE_MS)
                }
            })
        }
        postDelayed(resumeScroll, PAUSE_MS)
    }

    private fun stop() {
        cancelled = true
        animator?.cancel()
        animator = null
        cancelled = false
        removeCallbacks(resumeScroll)
    }

    private companion object {
        const val FADE_DP = 16f
        const val GAP_DP = 15f
        const val SPEED_DP_PER_SEC = 40f
        const val PAUSE_MS = 1_000L
        const val LOOP_PAUSE_MS = 2_000L
    }
}
