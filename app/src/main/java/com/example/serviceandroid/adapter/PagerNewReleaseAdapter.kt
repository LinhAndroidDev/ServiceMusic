package com.example.serviceandroid.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseAdapter
import com.example.serviceandroid.databinding.ItemPagerNewReleaseBinding
import com.example.serviceandroid.databinding.PagerNewReleaseBinding
import com.example.serviceandroid.model.Song

enum class TypeList {
    TYPE_NATIONAL,
    TYPE_NEW_UPDATE
}

class PagerNationalAdapter(private val context: Context, private val type: TypeList) :
    BaseAdapter<HashMap<Int, ArrayList<Song>>, PagerNewReleaseBinding>() {
    var pagerSong = HashMap<Int, ArrayList<Song>>()
    var onClickItem: ((Int) -> Unit)? = null
    private lateinit var adapterSong: PagerNewReleaseAdapter

    override fun getLayout(): Int = R.layout.pager_new_release

    @SuppressLint("NotifyDataSetChanged")
    fun resetList(list: HashMap<Int, ArrayList<Song>>) {
        pagerSong.clear()
        pagerSong = list
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: BaseViewHolder<PagerNewReleaseBinding>, position: Int) {
        adapterSong = PagerNewReleaseAdapter(context, type)
        pagerSong[position].let {
            if (it != null) {
                adapterSong.items = it
            }
        }
        holder.v.rcvPagerRelease.adapter = adapterSong
        adapterSong.onClickItem = {
            onClickItem?.invoke(it + position * 3)
        }
    }

    override fun getItemCount(): Int = pagerSong.size
}

class PagerNewReleaseAdapter(private val context: Context, private val type: TypeList) :
    BaseAdapter<Song, ItemPagerNewReleaseBinding>() {
    var onClickItem: ((Int) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_pager_new_release

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: BaseViewHolder<ItemPagerNewReleaseBinding>,
        position: Int
    ) {
        items[position].let {
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
                    holder.v.cvRoundImg.layoutParams.apply {
                        width = context.resources.getDimensionPixelSize(R.dimen.width_img)
                        height = context.resources.getDimensionPixelSize(R.dimen.height_img)
                    }
                }
            }
        }

        holder.itemView.setOnClickListener {
            onClickItem?.invoke(position)
        }
    }
}