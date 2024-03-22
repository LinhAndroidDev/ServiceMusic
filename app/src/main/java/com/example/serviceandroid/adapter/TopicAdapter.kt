package com.example.serviceandroid.adapter

import android.content.Context
import android.view.View
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseAdapter
import com.example.serviceandroid.databinding.ItemTopicBinding
import com.example.serviceandroid.model.Topic

class TopicAdapter(private val context: Context) : BaseAdapter<Topic, ItemTopicBinding>() {
    override fun getLayout(): Int = R.layout.item_topic

    override fun onBindViewHolder(holder: BaseViewHolder<ItemTopicBinding>, position: Int) {
        holder.v.spaceView.visibility = if (position == 0) View.VISIBLE else View.GONE
        if (position == items.lastIndex) {
            holder.v.cvBackground.visibility = View.GONE
            holder.v.seeAll.visibility = View.VISIBLE
        } else {
            holder.v.cvBackground.visibility = View.VISIBLE
            holder.v.seeAll.visibility = View.GONE
        }
        items[position].apply {
            icon?.let { holder.v.icon.setImageResource(it) }
            color?.let { holder.v.cvBackground.setCardBackgroundColor(context.getColor(it)) }
        }
        holder.v.topic.text = items[position].topic
    }

}