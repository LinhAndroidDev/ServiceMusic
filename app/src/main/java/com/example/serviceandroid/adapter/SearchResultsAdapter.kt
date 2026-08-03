package com.example.serviceandroid.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.serviceandroid.R
import com.example.serviceandroid.databinding.ItemPagerNewReleaseBinding
import com.example.serviceandroid.databinding.ItemSearchSectionHeaderBinding
import com.example.serviceandroid.databinding.ItemSearchSingerBinding
import com.example.serviceandroid.model.Singer
import com.example.serviceandroid.model.Song

sealed class SearchResultRow {
    data class Header(val titleRes: Int) : SearchResultRow()
    data class SongRow(val song: Song) : SearchResultRow()
    data class SingerRow(val singer: Singer) : SearchResultRow()
}

class SearchResultsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var onClickSong: ((Song) -> Unit)? = null
    var onClickSongMore: ((Song) -> Unit)? = null
    var onClickSinger: ((Singer) -> Unit)? = null

    private val rows = mutableListOf<SearchResultRow>()

    fun submit(songs: List<Song>, singers: List<Singer>) {
        rows.clear()
        if (songs.isNotEmpty()) {
            rows.add(SearchResultRow.Header(R.string.search_section_songs))
            songs.forEach { rows.add(SearchResultRow.SongRow(it)) }
        }
        if (singers.isNotEmpty()) {
            rows.add(SearchResultRow.Header(R.string.search_section_artists))
            singers.forEach { rows.add(SearchResultRow.SingerRow(it)) }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is SearchResultRow.Header -> TYPE_HEADER
        is SearchResultRow.SongRow -> TYPE_SONG
        is SearchResultRow.SingerRow -> TYPE_SINGER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(ItemSearchSectionHeaderBinding.inflate(inflater, parent, false))
            TYPE_SONG -> SongVH(ItemPagerNewReleaseBinding.inflate(inflater, parent, false))
            else -> SingerVH(ItemSearchSingerBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is SearchResultRow.Header -> (holder as HeaderVH).bind(row.titleRes)
            is SearchResultRow.SongRow -> (holder as SongVH).bind(row.song)
            is SearchResultRow.SingerRow -> (holder as SingerVH).bind(row.singer)
        }
    }

    override fun getItemCount(): Int = rows.size

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
            binding.imgSong.load(song.thumbnailUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_grey_corner_5)
            }
            binding.tvNameSong.text = song.title
            binding.tvNameSinger.text = song.nameSinger
            binding.layoutIndex.visibility = android.view.View.GONE
            binding.imgFavourite.visibility = android.view.View.GONE
            binding.moreOption.visibility = android.view.View.VISIBLE
            binding.root.setOnClickListener { onClickSong?.invoke(song) }
            binding.moreOption.setOnClickListener { onClickSongMore?.invoke(song) }
        }
    }

    private inner class SingerVH(
        private val binding: ItemSearchSingerBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(singer: Singer) {
            binding.imgSingerAvatar.load(singer.avatarUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_circle)
                error(R.drawable.ic_circle)
            }
            binding.tvSingerName.text = singer.name
            binding.root.setOnClickListener { onClickSinger?.invoke(singer) }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_SONG = 1
        private const val TYPE_SINGER = 2
    }
}
