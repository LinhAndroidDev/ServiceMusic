package com.example.serviceandroid.fragment.music

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceandroid.databinding.ItemMusicLyricsPageBinding
import com.example.serviceandroid.databinding.ItemMusicPlayerPageBinding
import com.example.serviceandroid.databinding.ItemMusicSingerPageBinding

class MusicNowPlayingPagerAdapter(
    private val onSingerPageBound: (ItemMusicSingerPageBinding) -> Unit,
    private val onPlayerPageBound: (ItemMusicPlayerPageBinding) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_SINGER = 0
        private const val TYPE_PLAYER = 1
        private const val TYPE_LYRICS = 2
    }

    var singerPageBinding: ItemMusicSingerPageBinding? = null
        private set
    var lyricsRecycler: RecyclerView? = null
        private set
    var lyricsEmpty: TextView? = null
        private set

    override fun getItemCount(): Int = 3

    override fun getItemViewType(position: Int): Int = when (position) {
        0 -> TYPE_SINGER
        1 -> TYPE_PLAYER
        else -> TYPE_LYRICS
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SINGER -> SingerVH(ItemMusicSingerPageBinding.inflate(inflater, parent, false))
            TYPE_PLAYER -> PlayerVH(ItemMusicPlayerPageBinding.inflate(inflater, parent, false))
            else -> LyricsVH(ItemMusicLyricsPageBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SingerVH -> {
                singerPageBinding = holder.binding
                if (!holder.boundOnce) {
                    holder.boundOnce = true
                    onSingerPageBound(holder.binding)
                }
            }
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

    private class SingerVH(val binding: ItemMusicSingerPageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var boundOnce: Boolean = false
    }

    private class PlayerVH(val binding: ItemMusicPlayerPageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var boundOnce: Boolean = false
    }

    private class LyricsVH(val binding: ItemMusicLyricsPageBinding) :
        RecyclerView.ViewHolder(binding.root)
}
