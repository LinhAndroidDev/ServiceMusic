package com.example.serviceandroid.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.serviceandroid.R
import com.example.serviceandroid.databinding.ItemListenRecentBinding
import com.example.serviceandroid.databinding.LayoutSeeAllBinding
import com.example.serviceandroid.model.Song

class ListenRecentAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var onClickItem: ((Song) -> Unit)? = null
    var onClickSeeAll: (() -> Unit)? = null

    private val songs = mutableListOf<Song>()
    private var showSeeAll = false

    fun submit(previewSongs: List<Song>, showSeeAll: Boolean) {
        songs.clear()
        songs.addAll(previewSongs)
        this.showSeeAll = showSeeAll
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (showSeeAll && position == songs.size) VIEW_TYPE_SEE_ALL else VIEW_TYPE_SONG
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SEE_ALL) {
            SeeAllViewHolder(LayoutSeeAllBinding.inflate(inflater, parent, false))
        } else {
            SongViewHolder(ItemListenRecentBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SongViewHolder -> holder.bind(songs[position])
            is SeeAllViewHolder -> holder.bind()
        }
    }

    override fun getItemCount(): Int = songs.size + if (showSeeAll) 1 else 0

    private inner class SongViewHolder(
        private val binding: ItemListenRecentBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song) {
            binding.recentSongTitle.text = song.title
            binding.recentSongSinger.text = song.nameSinger
            Glide.with(binding.recentSongThumbnail)
                .load(song.thumbnailUrl)
                .placeholder(R.drawable.ic_music)
                .error(R.drawable.ic_music)
                .centerCrop()
                .into(binding.recentSongThumbnail)
            itemView.setOnClickListener { onClickItem?.invoke(song) }
        }
    }

    private inner class SeeAllViewHolder(
        binding: LayoutSeeAllBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            itemView.setOnClickListener { onClickSeeAll?.invoke() }
        }
    }

    private companion object {
        const val VIEW_TYPE_SONG = 0
        const val VIEW_TYPE_SEE_ALL = 1
    }
}
