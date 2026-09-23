package com.example.serviceandroid.adapter

import android.content.Context
import android.view.View
import com.example.serviceandroid.R
import com.example.serviceandroid.base.BaseAdapter
import com.example.serviceandroid.databinding.ItemTopicBinding
import com.example.serviceandroid.model.Topic
import com.example.serviceandroid.model.TopicType

class TopicAdapter(private val context: Context) : BaseAdapter<Topic, ItemTopicBinding>() {
    var onClickItem: ((Topic) -> Unit)? = null

    override fun getLayout(): Int = R.layout.item_topic

    override fun onBindViewHolder(holder: BaseViewHolder<ItemTopicBinding>, position: Int) {
        val item = items[position]
        with(holder.v) {
            setSpaceViewVisibility(position, holder)
            setCardVisibility(item, holder)
            item.apply {
                icon?.let { holder.v.icon.setImageResource(it) }
                color?.let { cvBackground.setCardBackgroundColor(context.getColor(it)) }
            }
            topic.text = item.topic
        }
        holder.itemView.setOnClickListener {
            if (item.type != TopicType.SEE_ALL) {
                onClickItem?.invoke(item)
            }
        }
    }

    private fun setSpaceViewVisibility(position: Int, holder: BaseViewHolder<ItemTopicBinding>) {
        holder.v.spaceView.visibility = if (position == 0) View.VISIBLE else View.GONE
    }

    private fun setCardVisibility(item: Topic, holder: BaseViewHolder<ItemTopicBinding>) {
        with(holder.v) {
            if (item.type == TopicType.SEE_ALL) {
                cvBackground.visibility = View.GONE
                seeAll.root.visibility = View.VISIBLE
            } else {
                cvBackground.visibility = View.VISIBLE
                seeAll.root.visibility = View.GONE
            }
        }
    }
}