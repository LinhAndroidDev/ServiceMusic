package com.example.serviceandroid.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.serviceandroid.R
import com.example.serviceandroid.databinding.ItemPagerNewReleaseBinding
import com.example.serviceandroid.model.Song

class SearchSongAdapter : RecyclerView.Adapter<SearchSongAdapter.SongVH>() {

    var onClickSong: ((Song) -> Unit)? = null
    var onClickSongMore: ((Song) -> Unit)? = null

    private val items = mutableListOf<Song>()

    fun submit(songs: List<Song>) {
        items.clear()
        items.addAll(songs)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongVH {
        val binding = ItemPagerNewReleaseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return SongVH(binding)
    }

    override fun onBindViewHolder(holder: SongVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SongVH(
        private val binding: ItemPagerNewReleaseBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song) {
            binding.imgSong.load(song.thumbnailUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_grey_corner_5)
            }
            binding.tvNameSong.text = song.title
            binding.tvNameSinger.text = song.nameSinger
            binding.layoutIndex.visibility = View.GONE
            binding.imgFavourite.visibility = View.GONE
            binding.moreOption.visibility = View.VISIBLE
            binding.root.setOnClickListener { onClickSong?.invoke(song) }
            binding.moreOption.setOnClickListener { onClickSongMore?.invoke(song) }
        }
    }
}
