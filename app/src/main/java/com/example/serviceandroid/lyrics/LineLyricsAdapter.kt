package com.example.serviceandroid.lyrics

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceandroid.R

class LineLyricsAdapter(
    private var defaultColor: Int,
    private var activeColor: Int,
) : RecyclerView.Adapter<LineLyricsAdapter.VH>() {

    /** Set from the host (e.g. [FragmentMusic]); cleared on destroy to avoid leaking the host. */
    var onLineClickListener: ((TimedLyricLine) -> Unit)? = null

    private val lines = mutableListOf<TimedLyricLine>()
    private var selectedIndex: Int = -1

    override fun getItemCount(): Int = lines.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val root = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lyric_line, parent, false) as ViewGroup
        root.clipChildren = false
        val highlightBg = root.findViewById<View>(R.id.lyricHighlightBg)
        val tv = root.findViewById<TextView>(R.id.tvLine)
        return VH(root, highlightBg, tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(
            lines[position],
            position == selectedIndex,
            defaultColor,
            activeColor,
            onLineClickListener,
        )
    }

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.size == 1 && payloads[0] == PAYLOAD_SELECTION) {
            holder.bindSelectionOnly(
                position == selectedIndex,
                defaultColor,
                activeColor,
            )
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
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
     * Highlights [index] (-1 = none). Notifies only changed rows with a selection payload
     * so color/scale can animate without rebinding text.
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
        if (old in lines.indices) notifyItemChanged(old, PAYLOAD_SELECTION)
        if (selectedIndex in lines.indices) notifyItemChanged(selectedIndex, PAYLOAD_SELECTION)
    }

    class VH(
        root: ViewGroup,
        private val highlightBg: View,
        private val tv: TextView,
    ) : RecyclerView.ViewHolder(root) {

        private val argbEvaluator = ArgbEvaluator()

        init {
            tv.scaleX = IDLE_SCALE
            tv.scaleY = IDLE_SCALE
            highlightBg.alpha = 0f
            highlightBg.visibility = View.VISIBLE
            highlightBg.setBackgroundResource(R.drawable.bg_lyric_line_highlight)
        }

        fun bind(
            line: TimedLyricLine,
            selected: Boolean,
            defaultColor: Int,
            activeColor: Int,
            onLineClick: ((TimedLyricLine) -> Unit)?,
        ) {
            tv.animate().cancel()
            highlightBg.animate().cancel()
            cancelColorAnimator(tv)
            tv.text = line.text
            tv.setTextColor(if (selected) activeColor else defaultColor)
            tv.scaleX = if (selected) HIGHLIGHT_SCALE else IDLE_SCALE
            tv.scaleY = if (selected) HIGHLIGHT_SCALE else IDLE_SCALE
            applyPivotLeftAligned(tv)
            highlightBg.alpha = if (selected) 1f else 0f
            tv.setShadowLayer(
                if (selected) 4f * tv.resources.displayMetrics.density else 0f,
                0f,
                if (selected) 1f * tv.resources.displayMetrics.density else 0f,
                if (selected) Color.argb(60, 40, 40, 40) else Color.TRANSPARENT,
            )
            val listener = onLineClick
            itemView.setOnClickListener(
                if (listener != null) View.OnClickListener { listener(line) } else null,
            )
        }

        fun bindSelectionOnly(selected: Boolean, defaultColor: Int, activeColor: Int) {
            tv.animate().cancel()
            highlightBg.animate().cancel()
            cancelColorAnimator(tv)
            val targetColor = if (selected) activeColor else defaultColor
            animateTextColor(tv, tv.currentTextColor, targetColor, TRANSITION_DURATION_MS)
            animateTextScaleForSelection(selected)
            animateHighlightChrome(selected)
        }

        private fun animateTextScaleForSelection(selected: Boolean) {
            applyPivotLeftAligned(tv)
            val target = if (selected) HIGHLIGHT_SCALE else IDLE_SCALE
            // Animate from current scale — avoid snapping to IDLE first (causes visible jump).
            tv.animate()
                .scaleX(target)
                .scaleY(target)
                .setDuration(TRANSITION_DURATION_MS)
                .setInterpolator(SOFT_INTERPOLATOR)
                .start()
        }

        private fun animateHighlightChrome(selected: Boolean) {
            val d = tv.resources.displayMetrics.density
            val targetAlpha = if (selected) 1f else 0f
            highlightBg.animate()
                .alpha(targetAlpha)
                .setDuration(TRANSITION_DURATION_MS)
                .setInterpolator(SOFT_INTERPOLATOR)
                .start()
            // Soft shadow: set once toward the end state (avoid mid-scroll layout thrash).
            if (selected) {
                tv.setShadowLayer(4f * d, 0f, 1f * d, Color.argb(60, 40, 40, 40))
            } else {
                tv.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            }
        }

        private fun cancelColorAnimator(view: TextView) {
            (view.getTag(R.id.tag_lyric_line_color_animator) as? ValueAnimator)?.cancel()
            view.setTag(R.id.tag_lyric_line_color_animator, null)
        }

        private fun animateTextColor(view: TextView, from: Int, to: Int, durationMs: Long) {
            if (from == to) {
                view.setTextColor(to)
                return
            }
            cancelColorAnimator(view)
            val anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = durationMs
                interpolator = SOFT_INTERPOLATOR
                addUpdateListener { a ->
                    val t = a.animatedValue as Float
                    val color = argbEvaluator.evaluate(t, from, to) as Int
                    view.setTextColor(color)
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (view.getTag(R.id.tag_lyric_line_color_animator) === animation) {
                            view.setTag(R.id.tag_lyric_line_color_animator, null)
                        }
                        view.setTextColor(to)
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        // Keep current interpolated color; don't snap on cancel mid-transition.
                    }
                })
            }
            view.setTag(R.id.tag_lyric_line_color_animator, anim)
            anim.start()
        }

        /** Scale text from the start edge so lines stay left-aligned. */
        private fun applyPivotLeftAligned(view: TextView) {
            view.pivotX = view.paddingStart.toFloat()
            val h = view.height
            if (h > 0) view.pivotY = h / 2f
        }

        companion object {
            /** Softer idle/active delta to avoid fighting auto-scroll visually. */
            private const val IDLE_SCALE = 0.94f
            private const val HIGHLIGHT_SCALE = 1.03f
            private const val TRANSITION_DURATION_MS = 420L

            private val SOFT_INTERPOLATOR = PathInterpolator(0.33f, 0f, 0.2f, 1f)
        }
    }

    companion object {
        private const val PAYLOAD_SELECTION = "lyric_selection"
    }
}
