package com.example.serviceandroid.fragment.music

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceandroid.databinding.ItemMusicLyricsPageBinding
import com.example.serviceandroid.databinding.ItemMusicPlayerPageBinding

class MusicNowPlayingPagerAdapter(
    private val onPlayerPageBound: (ItemMusicPlayerPageBinding) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_PLAYER = 0
        private const val TYPE_LYRICS = 1
    }

    var lyricsRecycler: RecyclerView? = null
        private set
    var lyricsEmpty: TextView? = null
        private set

    override fun getItemCount(): Int = 2

    override fun getItemViewType(position: Int): Int =
        if (position == 0) TYPE_PLAYER else TYPE_LYRICS

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_PLAYER) {
            PlayerVH(ItemMusicPlayerPageBinding.inflate(inflater, parent, false))
        } else {
            LyricsVH(ItemMusicLyricsPageBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PlayerVH -> {
                if (!holder.boundOnce) {
                    holder.boundOnce = true
                    onPlayerPageBound(holder.binding)
                }
            }
            is LyricsVH -> {
                lyricsRecycler = holder.binding.rvLyricsLines
                lyricsEmpty = holder.binding.tvLyricsEmpty
            }
        }
    }

    private class PlayerVH(val binding: ItemMusicPlayerPageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var boundOnce: Boolean = false
    }

    private class LyricsVH(val binding: ItemMusicLyricsPageBinding) :
        RecyclerView.ViewHolder(binding.root)
}
