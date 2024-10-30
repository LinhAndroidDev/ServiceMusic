package com.example.serviceandroid.custom

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

open class CircularLayoutManager(context: Context?, orientation: Int, reverseLayout: Boolean = false) :
    LinearLayoutManager(context, orientation, reverseLayout) {

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        val scrolled = super.scrollHorizontallyBy(dx, recycler, state)
        handleCircularScroll(dx, recycler, state)
        return scrolled
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        val scrolled = super.scrollVerticallyBy(dy, recycler, state)
        handleCircularScroll(dy, recycler, state)
        return scrolled
    }

    private fun handleCircularScroll(d: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        if (childCount == 0) return

        if (d > 0) {
            val lastView = getChildAt(childCount - 1)
            if (lastView != null && getPosition(lastView) == itemCount - 1) {
                val firstView = getChildAt(0)
                if (firstView != null) {
                    scrollToPosition(0)
                }
            }
        } else {
            val firstView = getChildAt(0)
            if (firstView != null && getPosition(firstView) == 0) {
                val lastView = getChildAt(childCount - 1)
                if (lastView != null) {
                    scrollToPosition(itemCount - 1)
                }
            }
        }
    }

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        handleCircularScroll(0, null, state)
    }
}