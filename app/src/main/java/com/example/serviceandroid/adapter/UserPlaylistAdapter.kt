package com.example.serviceandroid.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceandroid.R
import com.example.serviceandroid.data.playlist.UserPlaylist
import com.example.serviceandroid.databinding.ItemUserPlaylistBinding
import com.example.serviceandroid.utils.loadSongThumbnail

class UserPlaylistAdapter : RecyclerView.Adapter<UserPlaylistAdapter.PlaylistViewHolder>() {
    var onClickItem: ((UserPlaylist) -> Unit)? = null
    private val playlists = mutableListOf<UserPlaylist>()

    fun submit(items: List<UserPlaylist>) {
        playlists.clear()
        playlists.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemUserPlaylistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(playlists[position])
    }

    override fun getItemCount(): Int = playlists.size

    inner class PlaylistViewHolder(
        private val binding: ItemUserPlaylistBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(playlist: UserPlaylist) {
            binding.playlistTitle.text = playlist.title
            binding.playlistMeta.text = if (playlist.isPublic) {
                itemView.context.getString(R.string.playlist_meta_public, playlist.songCount)
            } else {
                itemView.context.getString(R.string.playlist_meta_private, playlist.songCount)
            }
            binding.playlistCover.loadSongThumbnail(playlist.coverUrl)
            itemView.setOnClickListener { onClickItem?.invoke(playlist) }
        }
    }
}
