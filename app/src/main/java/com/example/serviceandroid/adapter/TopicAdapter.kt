package com.example.serviceandroid.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.example.serviceandroid.databinding.ItemTopicBinding
import com.example.serviceandroid.model.Topic

class TopicAdapter(private val context: Context) : Adapter<TopicAdapter.ViewHolder>() {
    var topics = arrayListOf<Topic>()

    inner class ViewHolder(val v: ItemTopicBinding) : RecyclerView.ViewHolder(v.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicAdapter.ViewHolder {
        val v = ItemTopicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: TopicAdapter.ViewHolder, position: Int) {
        holder.v.spaceView.visibility = if (position == 0) View.VISIBLE else View.GONE
        if (position == topics.lastIndex) {
            holder.v.cvBackground.visibility = View.GONE
            holder.v.seeAll.visibility = View.VISIBLE
        } else {
            holder.v.cvBackground.visibility = View.VISIBLE
            holder.v.seeAll.visibility = View.GONE
        }
        topics[position].icon?.let { holder.v.icon.setImageResource(it) }
        topics[position].color?.let { holder.v.cvBackground.setCardBackgroundColor(context.getColor(it)) }
        holder.v.topic.text = topics[position].topic
    }

    override fun getItemCount(): Int = topics.size

}