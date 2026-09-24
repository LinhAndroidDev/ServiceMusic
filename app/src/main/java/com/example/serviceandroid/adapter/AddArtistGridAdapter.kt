package com.example.serviceandroid.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceandroid.R
import com.example.serviceandroid.databinding.ItemAddArtistBinding
import com.example.serviceandroid.model.Singer
import com.example.serviceandroid.utils.loadSingerAvatar

class AddArtistGridAdapter : RecyclerView.Adapter<AddArtistGridAdapter.ArtistVH>() {

    var onClickSinger: ((Singer) -> Unit)? = null

    private val items = mutableListOf<Singer>()
    private var selectedIds: Set<String> = emptySet()

    fun submit(singers: List<Singer>, selected: Set<String>) {
        val sameItems = items == singers
        val previous = selectedIds
        items.clear()
        items.addAll(singers)
        selectedIds = selected
        if (!sameItems) {
            notifyDataSetChanged()
            return
        }
        singers.forEachIndexed { index, singer ->
            if ((singer.id in previous) != (singer.id in selected)) {
                notifyItemChanged(index)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistVH {
        val binding = ItemAddArtistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ArtistVH(binding)
    }

    override fun onBindViewHolder(holder: ArtistVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ArtistVH(
        private val binding: ItemAddArtistBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(singer: Singer) {
            val selected = singer.id in selectedIds
            binding.imgSingerAvatar.loadSingerAvatar(singer.avatarUrl)
            binding.imgSingerAvatar.setBackgroundResource(
                if (selected) R.drawable.bg_artist_avatar_ring else 0,
            )
            binding.imgSelected.isVisible = selected
            binding.tvSingerName.text = singer.name
            binding.root.setOnClickListener { onClickSinger?.invoke(singer) }
        }
    }
}
