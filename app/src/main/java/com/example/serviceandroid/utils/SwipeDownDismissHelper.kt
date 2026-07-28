package com.example.serviceandroid.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.LinearLayout
import androidx.core.view.isInvisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import kotlin.math.abs

/**
 * Vertical swipe-down dismiss for a full-screen screen.
 * [handleView] receives the gesture (via [SwipeDownHandleLayout]); [contentView] is translated.
 * Child clicks on the handle (e.g. back button) still work until a vertical drag begins.
 */
class SwipeDownDismissHelper(
    private val handleView: SwipeDownHandleLayout,
    private val contentView: View,
    private val onDismiss: () -> Unit,
) {
    private val touchSlop = ViewConfiguration.get(contentView.context).scaledTouchSlop
    private val minFlingVelocity =
        ViewConfiguration.get(contentView.context).scaledMinimumFlingVelocity.toFloat()
    private val interpolator = FastOutSlowInInterpolator()

    private var velocityTracker: VelocityTracker? = null
    private var downRawX = 0f
    private var downRawY = 0f
    private var dragging = false
    private var dismissed = false

    init {
        handleView.dismissHelper = this
    }

    fun detach() {
        contentView.animate().cancel()
        recycleVelocityTracker()
        handleView.dismissHelper = null
        if (!dismissed) {
            contentView.translationY = 0f
            contentView.alpha = 1f
            contentView.isInvisible = false
        }
    }

    fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (dismissed) return false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                contentView.animate().cancel()
                downRawX = ev.rawX
                downRawY = ev.rawY
                dragging = false
                obtainVelocityTracker().apply {
                    clear()
                    addMovement(ev)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                obtainVelocityTracker().addMovement(ev)
                val dy = ev.rawY - downRawY
                val dx = ev.rawX - downRawX
                if (!dragging && dy > touchSlop && abs(dy) > abs(dx)) {
                    dragging = true
                    handleView.parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                recycleVelocityTracker()
                dragging = false
            }
        }
        return false
    }

    fun onTouchEvent(ev: MotionEvent): Boolean {
        if (dismissed) return false
        obtainVelocityTracker().addMovement(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = ev.rawX
                downRawY = ev.rawY
                dragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = (ev.rawY - downRawY).coerceAtLeast(0f)
                val dx = ev.rawX - downRawX
                if (!dragging) {
                    if (dy > touchSlop && abs(dy) > abs(dx)) {
                        dragging = true
                        handleView.parent?.requestDisallowInterceptTouchEvent(true)
                    } else {
                        return true
                    }
                }
                applyDrag(dy)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasDragging = dragging
                dragging = false
                if (wasDragging) {
                    finishDrag()
                }
                recycleVelocityTracker()
                return wasDragging
            }
        }
        return false
    }

    private fun applyDrag(dy: Float) {
        val height = contentView.height.coerceAtLeast(1)
        val progress = (dy / height).coerceIn(0f, 1f)
        contentView.translationY = dy
        contentView.alpha = 1f - ALPHA_FADE_AMOUNT * progress
    }

    private fun finishDrag() {
        val height = contentView.height.coerceAtLeast(1)
        val translationY = contentView.translationY
        val tracker = velocityTracker
        tracker?.computeCurrentVelocity(1000)
        val velocityY = tracker?.yVelocity ?: 0f

        val shouldDismiss =
            translationY > height * DISMISS_FRACTION ||
                velocityY > minFlingVelocity * FLING_MULTIPLIER

        if (shouldDismiss) {
            animateDismiss(height.toFloat())
        } else {
            animateSettleBack()
        }
    }

    private fun animateSettleBack() {
        contentView.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(SETTLE_BACK_MS)
            .setInterpolator(interpolator)
            .start()
    }

    private fun animateDismiss(targetY: Float) {
        dismissed = true
        contentView.animate()
            .translationY(targetY)
            .alpha(0f)
            .setDuration(DISMISS_MS)
            .setInterpolator(interpolator)
            .withEndAction {
                contentView.isInvisible = true
                onDismiss()
            }
            .start()
    }

    private fun obtainVelocityTracker(): VelocityTracker {
        return velocityTracker ?: VelocityTracker.obtain().also { velocityTracker = it }
    }

    private fun recycleVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    companion object {
        private const val DISMISS_FRACTION = 0.25f
        private const val ALPHA_FADE_AMOUNT = 0.35f
        private const val FLING_MULTIPLIER = 1.5f
        private const val SETTLE_BACK_MS = 200L
        private const val DISMISS_MS = 220L
    }
}

/**
 * Top chrome container that intercepts vertical drag for [SwipeDownDismissHelper]
 * while still allowing child clicks (e.g. back button) on short taps.
 */
class SwipeDownHandleLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    var dismissHelper: SwipeDownDismissHelper? = null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val helper = dismissHelper
        if (helper != null && helper.onInterceptTouchEvent(ev)) {
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val helper = dismissHelper
        if (helper != null && helper.onTouchEvent(ev)) {
            return true
        }
        return super.onTouchEvent(ev)
    }
}
