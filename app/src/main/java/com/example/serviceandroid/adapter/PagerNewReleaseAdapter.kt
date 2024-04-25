package com.example.serviceandroid.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import coil.load
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
    private val adapterSong by lazy { PagerNewReleaseAdapter(context, type) }

    override fun getLayout(): Int = R.layout.pager_new_release

    @SuppressLint("NotifyDataSetChanged")
    fun resetList(list: HashMap<Int, ArrayList<Song>>) {
        pagerSong.clear()
        pagerSong = list
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: BaseViewHolder<PagerNewReleaseBinding>, position: Int) {
        pagerSong[position].let {
            if (it != null) {
                adapterSong.items = it
            }
        }
        holder.v.rcvPagerRelease.adapter = adapterSong
        adapterSong.onClickItem = {
            onClickItem?.invoke(it)
        }
    }

    override fun getItemCount(): Int = pagerSong.size
}

class PagerNewReleaseAdapter(private val context: Context, private val type: TypeList) :
    BaseAdapter<Song, ItemPagerNewReleaseBinding>() {
    var onClickItem: ((Int) -> Unit)? = null
    var onClickUnFavourite: ((Int) -> Unit)? = null
    var isFavourite = false

    override fun getLayout(): Int = R.layout.item_pager_new_release

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: BaseViewHolder<ItemPagerNewReleaseBinding>,
        position: Int
    ) {
        with(holder.v) {
            items[position].let { item ->
                imgSong.load(item.avatar) {
                    crossfade(true)
                    placeholder(R.drawable.bg_grey_corner_5)
                }
                tvNameSong.text = item.title
                tvNameSinger.text = item.nameSinger
                holder.itemView.setOnClickListener {
                    onClickItem?.invoke(item.id)
                }
                holder.v.imgFavourite.setOnClickListener {
                    onClickUnFavourite?.invoke(position)
                }
            }

            when (type) {
                TypeList.TYPE_NATIONAL -> {
                    if(isFavourite) imgFavourite.isVisible = true
                    layoutIndex.visibility = View.GONE
                    tvNameSong.setTextColor(context.getColor(R.color.text_black))
                    tvTime.visibility = View.VISIBLE
                }

                else -> {
                    layoutIndex.visibility = View.VISIBLE
                    tvIndex.text = "${position + 1}"
                    tvNameSong.setTextColor(context.getColor(R.color.text_white))
                    tvTime.visibility = View.GONE
                    cvRoundImg.layoutParams.apply {
                        width = context.resources.getDimensionPixelSize(R.dimen.width_img)
                        height = context.resources.getDimensionPixelSize(R.dimen.height_img)
                    }
                }
            }
        }
    }
}