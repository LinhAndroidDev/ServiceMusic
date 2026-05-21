package com.example.serviceandroid.lyrics

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceandroid.R

class LineLyricsAdapter(
    private var defaultColor: Int,
    private var activeColor: Int,
) : RecyclerView.Adapter<LineLyricsAdapter.VH>() {

    private val lines = mutableListOf<TimedLyricLine>()
    private var selectedIndex: Int = -1

    override fun getItemCount(): Int = lines.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val root = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lyric_line, parent, false) as ViewGroup
        root.clipChildren = false
        val tv = root.findViewById<TextView>(R.id.tvLine)
        return VH(root, tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(lines[position], position == selectedIndex, defaultColor, activeColor)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateColors(defaultColor: Int, activeColor: Int) {
        this.defaultColor = defaultColor
        this.activeColor = activeColor
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitLines(newLines: List<TimedLyricLine>) {
        lines.clear()
        lines.addAll(newLines)
        selectedIndex = -1
        notifyDataSetChanged()
    }

    /**
     * Highlights [index] (-1 = none). Notifies only changed rows.
     */
    fun setActiveLine(index: Int) {
        if (lines.isEmpty()) {
            selectedIndex = -1
            return
        }
        val safe = if (index < 0) -1 else index.coerceIn(0, lines.lastIndex)
        if (safe == selectedIndex) return
        val old = selectedIndex
        selectedIndex = safe
        if (old in lines.indices) notifyItemChanged(old)
        if (selectedIndex in lines.indices) notifyItemChanged(selectedIndex)
    }

    class VH(
        root: ViewGroup,
        private val tv: TextView,
    ) : RecyclerView.ViewHolder(root) {

        init {
            tv.scaleX = IDLE_SCALE
            tv.scaleY = IDLE_SCALE
        }

        fun bind(line: TimedLyricLine, selected: Boolean, defaultColor: Int, activeColor: Int) {
            tv.animate().cancel()
            tv.text = line.text
            tv.setTextColor(if (selected) activeColor else defaultColor)
            applyHighlightChrome(tv, selected)
            tv.post {
                applyPivotLeftAligned(tv)
                if (selected) {
                    tv.scaleX = IDLE_SCALE
                    tv.scaleY = IDLE_SCALE
                    tv.animate()
                        .scaleX(HIGHLIGHT_SCALE)
                        .scaleY(HIGHLIGHT_SCALE)
                        .setDuration(SCALE_DURATION_MS)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                } else {
                    tv.animate()
                        .scaleX(IDLE_SCALE)
                        .scaleY(IDLE_SCALE)
                        .setDuration(SCALE_DURATION_MS)
                        .setInterpolator(DecelerateInterpolator())
                        .start()
                }
            }
        }

        private fun applyHighlightChrome(tv: TextView, selected: Boolean) {
            val d = tv.resources.displayMetrics.density
            if (selected) {
                tv.setBackgroundResource(R.drawable.bg_lyric_line_highlight)
                tv.setShadowLayer(6f * d, 0f, 1f * d, Color.argb(90, 40, 40, 40))
            } else {
                tv.background = null
                tv.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            }
        }

        /** Scale from the logical start edge so lines stay start-aligned. */
        private fun applyPivotLeftAligned(view: TextView) {
            view.pivotX = view.paddingStart.toFloat()
            val h = view.height
            if (h > 0) view.pivotY = h / 2f
        }

        companion object {
            /** Slightly smaller than “full” size when not highlighted */
            private const val IDLE_SCALE = 0.88f
            /** Size when highlighted (after animate from [IDLE_SCALE]) */
            private const val HIGHLIGHT_SCALE = 1.08f
            private const val SCALE_DURATION_MS = 220L
        }
    }
}
