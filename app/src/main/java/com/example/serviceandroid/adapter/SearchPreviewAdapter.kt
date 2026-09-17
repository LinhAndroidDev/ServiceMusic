package com.example.serviceandroid.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.serviceandroid.R
import com.example.serviceandroid.databinding.ItemPagerNewReleaseBinding
import com.example.serviceandroid.databinding.ItemSearchRelatedNameBinding
import com.example.serviceandroid.databinding.ItemSearchSectionHeaderBinding
import com.example.serviceandroid.databinding.ItemSearchSingerBinding
import com.example.serviceandroid.model.Singer
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.loadSingerAvatar
import com.example.serviceandroid.utils.loadSongThumbnail

class SearchPreviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var onClickName: ((String) -> Unit)? = null
    var onClickSong: ((Song) -> Unit)? = null
    var onClickSongMore: ((Song) -> Unit)? = null
    var onClickSinger: ((Singer) -> Unit)? = null

    private val rows = mutableListOf<Row>()

    fun submit(names: List<String>, songs: List<Song>, singers: List<Singer>) {
        rows.clear()
        names.forEach { rows.add(Row.Name(it)) }
        if (songs.isNotEmpty()) {
            rows.add(Row.Header(R.string.search_section_songs))
            songs.forEach { rows.add(Row.SongItem(it)) }
        }
        if (singers.isNotEmpty()) {
            rows.add(Row.Header(R.string.search_section_artists))
            singers.forEach { rows.add(Row.SingerItem(it)) }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is Row.Name -> TYPE_NAME
        is Row.Header -> TYPE_HEADER
        is Row.SongItem -> TYPE_SONG
        is Row.SingerItem -> TYPE_SINGER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_NAME -> NameVH(ItemSearchRelatedNameBinding.inflate(inflater, parent, false))
            TYPE_HEADER -> HeaderVH(ItemSearchSectionHeaderBinding.inflate(inflater, parent, false))
            TYPE_SONG -> SongVH(ItemPagerNewReleaseBinding.inflate(inflater, parent, false))
            else -> SingerVH(ItemSearchSingerBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Name -> (holder as NameVH).bind(row.text)
            is Row.Header -> (holder as HeaderVH).bind(row.titleRes)
            is Row.SongItem -> (holder as SongVH).bind(row.song)
            is Row.SingerItem -> (holder as SingerVH).bind(row.singer)
        }
    }

    override fun getItemCount(): Int = rows.size

    private inner class NameVH(
        private val binding: ItemSearchRelatedNameBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(name: String) {
            binding.tvRelatedName.text = name
            binding.root.setOnClickListener { onClickName?.invoke(name) }
        }
    }

    private inner class HeaderVH(
        private val binding: ItemSearchSectionHeaderBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(titleRes: Int) {
            binding.tvSectionTitle.setText(titleRes)
        }
    }

    private inner class SongVH(
        private val binding: ItemPagerNewReleaseBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song) {
            binding.imgSong.loadSongThumbnail(song.thumbnailUrl)
            binding.tvNameSong.text = song.title
            binding.tvNameSinger.text = song.nameSinger
            binding.layoutIndex.visibility = View.GONE
            binding.imgFavourite.visibility = View.GONE
            binding.moreOption.visibility = View.VISIBLE
            binding.root.setOnClickListener { onClickSong?.invoke(song) }
            binding.moreOption.setOnClickListener { onClickSongMore?.invoke(song) }
        }
    }

    private inner class SingerVH(
        private val binding: ItemSearchSingerBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(singer: Singer) {
            binding.imgSingerAvatar.loadSingerAvatar(singer.avatarUrl)
            binding.tvSingerName.text = singer.name
            binding.root.setOnClickListener { onClickSinger?.invoke(singer) }
        }
    }

    private sealed class Row {
        data class Name(val text: String) : Row()
        data class Header(val titleRes: Int) : Row()
        data class SongItem(val song: Song) : Row()
        data class SingerItem(val singer: Singer) : Row()
    }

    private companion object {
        const val TYPE_NAME = 0
        const val TYPE_HEADER = 1
        const val TYPE_SONG = 2
        const val TYPE_SINGER = 3
    }
}
