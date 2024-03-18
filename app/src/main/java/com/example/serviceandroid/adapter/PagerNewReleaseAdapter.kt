package com.example.serviceandroid.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.bumptech.glide.Glide
import com.example.serviceandroid.R
import com.example.serviceandroid.databinding.ItemPagerNewReleaseBinding
import com.example.serviceandroid.databinding.PagerNewReleaseBinding
import com.example.serviceandroid.model.Song

enum class TypeList {
    TYPE_NATIONAL,
    TYPE_NEW_UPDATE
}

class PagerNationalAdapter(private val context: Context, private val type: TypeList) :
    Adapter<PagerNationalAdapter.ViewHolder>() {
    var pagerSong = HashMap<Int, ArrayList<Song>>()
    var onClickItem: ((Int) -> Unit)? = null

    inner class ViewHolder(val v: PagerNewReleaseBinding) : RecyclerView.ViewHolder(v.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): PagerNationalAdapter.ViewHolder = ViewHolder(
        PagerNewReleaseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun onBindViewHolder(holder: PagerNationalAdapter.ViewHolder, position: Int) {
        val adapter = PagerNewReleaseAdapter(context, type)
        pagerSong[position].let {
            if (it != null) {
                adapter.songs = it
            }
        }
        holder.v.rcvPagerRelease.adapter = adapter
        adapter.onClickItem = {
            onClickItem?.invoke(it + position * 3)
        }

    }

    override fun getItemCount(): Int = pagerSong.size
}

class PagerNewReleaseAdapter(private val context: Context, private val type: TypeList) :
    Adapter<PagerNewReleaseAdapter.ViewHolder>() {
    var songs: ArrayList<Song> = arrayListOf()
    var onClickItem: ((Int) -> Unit)? = null

    inner class ViewHolder(val v: ItemPagerNewReleaseBinding) : RecyclerView.ViewHolder(v.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): PagerNewReleaseAdapter.ViewHolder = ViewHolder(
        ItemPagerNewReleaseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PagerNewReleaseAdapter.ViewHolder, position: Int) {
        songs[position].let {
            Glide.with(context)
                .load(it.avatar)
                .error(R.mipmap.ic_launcher)
                .placeholder(R.mipmap.ic_launcher)
                .into(holder.v.imgSong)

            holder.v.tvNameSong.text = it.title
            holder.v.tvNameSinger.text = it.nameSinger

            when (type) {
                TypeList.TYPE_NATIONAL -> {
                    holder.v.layoutIndex.visibility = View.GONE
                    holder.v.tvNameSong.setTextColor(context.getColor(R.color.text_black))
                    holder.v.tvTime.visibility = View.VISIBLE
                }

                else -> {
                    holder.v.layoutIndex.visibility = View.VISIBLE
                    holder.v.tvIndex.text = "${position + 1}"
                    holder.v.tvNameSong.setTextColor(context.getColor(R.color.text_white))
                    holder.v.tvTime.visibility = View.GONE
                    holder.v.cvRoundImg.layoutParams.width =
                        context.resources.getDimensionPixelSize(R.dimen.width_img)
                    holder.v.cvRoundImg.layoutParams.height =
                        context.resources.getDimensionPixelSize(R.dimen.height_img)
                }
            }
        }

        holder.itemView.setOnClickListener {
            onClickItem?.invoke(position)
        }
    }

    override fun getItemCount(): Int = songs.size
}