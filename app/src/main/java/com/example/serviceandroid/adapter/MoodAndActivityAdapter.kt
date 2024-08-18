package com.example.serviceandroid.adapter

import androidx.core.view.isVisible
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseAdapter
import com.example.serviceandroid.databinding.ItemMoodAndActivityBinding

class MoodAndActivityAdapter : BaseAdapter<Int, ItemMoodAndActivityBinding>() {
    override fun getItemCount(): Int = 6
    override fun getLayout(): Int = R.layout.item_mood_and_activity

    override fun onBindViewHolder(
        holder: BaseViewHolder<ItemMoodAndActivityBinding>,
        position: Int,
    ) {
        holder.v.layoutMargin.isVisible = position == 0
    }
}