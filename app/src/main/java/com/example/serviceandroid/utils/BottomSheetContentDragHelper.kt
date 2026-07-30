package com.example.serviceandroid.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.AbsSeekBar
import android.widget.Button
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import kotlin.math.abs

/**
 * Drag-to-dismiss for a full-screen [BottomSheetDialog] content area.
 *
 * Vertical drag is allowed over most of the sheet; interactive controls (buttons, seek bars,
 * vertical lists, tabs) keep their own touch handling.
 */
class BottomSheetContentDragHelper(
    private val sheetView: View,
    private val contentRoot: BottomSheetDragLayout,
    private val dismissFraction: Float,
    private val onDismiss: () -> Unit,
) {
    private val touchSlop = ViewConfiguration.get(sheetView.context).scaledTouchSlop
    private val minFlingVelocity =
        ViewConfiguration.get(sheetView.context).scaledMinimumFlingVelocity.toFloat()
    private val interpolator = FastOutSlowInInterpolator()

    private var velocityTracker: VelocityTracker? = null
    private var downRawX = 0f
    private var downRawY = 0f
    private var downXInRoot = 0f
    private var downYInRoot = 0f
    private var dragging = false
    private var dismissed = false
    private var allowDragFromDown = false
    private var peakTranslationY = 0f

    init {
        contentRoot.dragHelper = this
    }

    fun detach() {
        sheetView.animate().cancel()
        recycleVelocityTracker()
        contentRoot.dragHelper = null
        if (!dismissed) {
            sheetView.translationY = 0f
            sheetView.alpha = 1f
        }
    }

    fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (dismissed) return false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                sheetView.animate().cancel()
                downRawX = ev.rawX
                downRawY = ev.rawY
                downXInRoot = ev.x
                downYInRoot = ev.y
                dragging = false
                peakTranslationY = 0f
                allowDragFromDown = !isInteractiveTarget(contentRoot, downXInRoot, downYInRoot)
                obtainVelocityTracker().apply {
                    clear()
                    addMovement(ev)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!allowDragFromDown) return false
                obtainVelocityTracker().addMovement(ev)
                val dy = ev.rawY - downRawY
                val dx = ev.rawX - downRawX
                if (!dragging && dy > touchSlop && abs(dy) > abs(dx)) {
                    dragging = true
                    contentRoot.parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                recycleVelocityTracker()
                dragging = false
                allowDragFromDown = false
                peakTranslationY = 0f
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
                downXInRoot = ev.x
                downYInRoot = ev.y
                dragging = false
                peakTranslationY = 0f
                allowDragFromDown = !isInteractiveTarget(contentRoot, downXInRoot, downYInRoot)
                return allowDragFromDown
            }
            MotionEvent.ACTION_MOVE -> {
                if (!allowDragFromDown) return false
                val dy = (ev.rawY - downRawY).coerceAtLeast(0f)
                val dx = ev.rawX - downRawX
                if (!dragging) {
                    if (dy > touchSlop && abs(dy) > abs(dx)) {
                        dragging = true
                        contentRoot.parent?.requestDisallowInterceptTouchEvent(true)
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
                allowDragFromDown = false
                if (wasDragging) {
                    finishDrag()
                }
                peakTranslationY = 0f
                recycleVelocityTracker()
                return wasDragging
            }
        }
        return false
    }

    private fun applyDrag(dy: Float) {
        val height = sheetView.height.coerceAtLeast(1)
        val progress = (dy / height).coerceIn(0f, 1f)
        peakTranslationY = maxOf(peakTranslationY, dy)
        sheetView.translationY = dy
        sheetView.alpha = 1f - ALPHA_FADE_AMOUNT * progress
    }

    private fun finishDrag() {
        val height = sheetView.height.coerceAtLeast(1)
        val translationY = sheetView.translationY
        velocityTracker?.computeCurrentVelocity(1000)
        val velocityY = velocityTracker?.yVelocity ?: 0f
        val flingThreshold = minFlingVelocity * FLING_MULTIPLIER
        val movedBackUp = translationY < peakTranslationY - touchSlop

        // Any upward intent (velocity or finger moving back up) settles to full.
        val shouldDismiss = when {
            velocityY < 0f || movedBackUp -> false
            velocityY > flingThreshold -> true
            else -> translationY > height * dismissFraction
        }

        if (shouldDismiss) {
            animateDismiss(height.toFloat())
        } else {
            animateSettleBack()
        }
    }

    private fun animateSettleBack() {
        sheetView.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(SETTLE_BACK_MS)
            .setInterpolator(interpolator)
            .start()
    }

    private fun animateDismiss(targetY: Float) {
        dismissed = true
        sheetView.animate()
            .translationY(targetY)
            .alpha(0f)
            .setDuration(DISMISS_MS)
            .setInterpolator(interpolator)
            .withEndAction(onDismiss)
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
        private const val ALPHA_FADE_AMOUNT = 0.35f
        private const val FLING_MULTIPLIER = 1.5f
        private const val SETTLE_BACK_MS = 200L
        private const val DISMISS_MS = 220L

        /**
         * True when [x]/[y] (in [root] coords) is over a control that should keep the gesture
         * (buttons, seek bars, tabs, vertical lists).
         */
        fun isInteractiveTarget(root: ViewGroup, x: Float, y: Float): Boolean {
            val hit = findDeepestVisibleChild(root, x, y) ?: return false
            var current: View? = hit
            while (current != null && current !== root) {
                when (current) {
                    is AbsSeekBar -> return true
                    is TabLayout -> return true
                    is Button, is ImageButton, is MaterialButton -> return true
                    is RecyclerView -> {
                        val orientation =
                            (current.layoutManager as? LinearLayoutManager)?.orientation
                        // Vertical lists (lyrics) keep scroll; ViewPager2's horizontal RV does not block.
                        if (orientation == RecyclerView.VERTICAL) return true
                    }
                }
                if (isActionControl(current)) {
                    return true
                }
                current = current.parent as? View
            }
            return false
        }

        private fun isActionControl(view: View): Boolean {
            if (!view.isClickable && !view.isLongClickable && !view.hasOnClickListeners()) {
                return false
            }
            // Large page containers / pager should not block sheet drag.
            if (view is ViewPager2) return false
            if (view is RecyclerView) return false
            return view.hasOnClickListeners() ||
                view is ImageButton ||
                view is Button ||
                view is MaterialButton ||
                (view.isClickable && view !is ViewGroup)
        }

        private fun findDeepestVisibleChild(group: ViewGroup, x: Float, y: Float): View? {
            for (i in group.childCount - 1 downTo 0) {
                val child = group.getChildAt(i)
                if (!child.isVisible) continue
                val left = child.left - child.translationX
                val top = child.top - child.translationY
                val right = left + child.width
                val bottom = top + child.height
                if (x !in left..<right || y < top || y >= bottom) continue
                val childX = x - left + child.scrollX
                val childY = y - top + child.scrollY
                if (child is ViewGroup) {
                    findDeepestVisibleChild(child, childX, childY)?.let { return it }
                }
                return child
            }
            return null
        }
    }
}

/**
 * Root content layout that forwards vertical dismiss drags to [BottomSheetContentDragHelper]
 * while leaving taps/drags on action controls to their children.
 */
class BottomSheetDragLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr) {

    var dragHelper: BottomSheetContentDragHelper? = null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val helper = dragHelper
        if (helper != null && helper.onInterceptTouchEvent(ev)) {
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val helper = dragHelper
        if (helper != null && helper.onTouchEvent(ev)) {
            return true
        }
        return super.onTouchEvent(ev)
    }
}
