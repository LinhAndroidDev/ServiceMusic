package com.example.serviceandroid.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceandroid.databinding.ItemEditPlaylistSongBinding
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.loadSongThumbnail

class EditPlaylistSongAdapter : RecyclerView.Adapter<EditPlaylistSongAdapter.SongViewHolder>() {
    var onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null
    private val songs = mutableListOf<Song>()

    fun currentSongs(): List<Song> = songs.toList()

    @SuppressLint("NotifyDataSetChanged")
    fun submit(items: List<Song>) {
        songs.clear()
        songs.addAll(items)
        notifyDataSetChanged()
    }

    fun moveItem(from: Int, to: Int) {
        if (from !in songs.indices || to !in songs.indices) return
        val item = songs.removeAt(from)
        songs.add(to, item)
        notifyItemMoved(from, to)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemEditPlaylistSongBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(songs[position], onStartDrag)
    }

    fun setDragging(holder: RecyclerView.ViewHolder, dragging: Boolean) {
        (holder as? SongViewHolder)?.setDragging(dragging)
    }

    override fun getItemCount(): Int = songs.size

    class SongViewHolder(
        private val binding: ItemEditPlaylistSongBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            song: Song,
            onStartDrag: ((RecyclerView.ViewHolder) -> Unit)?,
        ) {
            binding.songTitle.text = song.title
            binding.songSinger.text = song.nameSinger
            binding.songCover.loadSongThumbnail(song.thumbnailUrl)
            binding.dragHandle.setOnLongClickListener {
                onStartDrag?.invoke(this)
                true
            }
        }

        fun setDragging(dragging: Boolean) {
            binding.shadowTop.visibility = if (dragging) View.VISIBLE else View.GONE
            binding.shadowBottom.visibility = if (dragging) View.VISIBLE else View.GONE
        }
    }
}
