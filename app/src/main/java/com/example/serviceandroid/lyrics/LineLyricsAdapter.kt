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
import android.view.animation.DecelerateInterpolator
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
        }

        fun bind(
            line: TimedLyricLine,
            selected: Boolean,
            defaultColor: Int,
            activeColor: Int,
            onLineClick: ((TimedLyricLine) -> Unit)?,
        ) {
            tv.animate().cancel()
            cancelColorAnimator(tv)
            tv.text = line.text
            if (selected) {
                tv.setTextColor(defaultColor)
                animateTextColor(tv, defaultColor, activeColor, COLOR_DURATION_MS)
            } else {
                tv.setTextColor(defaultColor)
            }
            applyHighlightChrome(highlightBg, tv, selected)
            tv.post { animateTextScaleForSelection(selected) }
            val listener = onLineClick
            itemView.setOnClickListener(
                if (listener != null) View.OnClickListener { listener(line) } else null,
            )
        }

        fun bindSelectionOnly(selected: Boolean, defaultColor: Int, activeColor: Int) {
            tv.animate().cancel()
            cancelColorAnimator(tv)
            val target = if (selected) activeColor else defaultColor
            animateTextColor(tv, tv.currentTextColor, target, COLOR_DURATION_MS)
            applyHighlightChrome(highlightBg, tv, selected)
            tv.post { animateTextScaleForSelection(selected) }
        }

        private fun animateTextScaleForSelection(selected: Boolean) {
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
                interpolator = COLOR_INTERPOLATOR
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
                        view.setTextColor(to)
                    }
                })
            }
            view.setTag(R.id.tag_lyric_line_color_animator, anim)
            anim.start()
        }

        private fun applyHighlightChrome(highlightBg: View, tv: TextView, selected: Boolean) {
            val d = tv.resources.displayMetrics.density
            if (selected) {
                highlightBg.setBackgroundResource(R.drawable.bg_lyric_line_highlight)
                highlightBg.visibility = View.VISIBLE
                highlightBg.alpha = 1f
                tv.setShadowLayer(6f * d, 0f, 1f * d, Color.argb(90, 40, 40, 40))
            } else {
                highlightBg.background = null
                highlightBg.visibility = View.GONE
                tv.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            }
        }

        /** Scale text from the start edge so lines stay left-aligned. */
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
            private const val COLOR_DURATION_MS = 300L

            /** Material “standard” easing (fast out, slow in). */
            private val COLOR_INTERPOLATOR = PathInterpolator(0.4f, 0f, 0.2f, 1f)
        }
    }

    companion object {
        private const val PAYLOAD_SELECTION = "lyric_selection"
    }
}
