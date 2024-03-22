package com.example.serviceandroid.base

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

abstract class BaseAdapter<T : Any, VB : ViewDataBinding> :
    RecyclerView.Adapter<BaseAdapter.BaseViewHolder<VB>>() {
    var items = arrayListOf<T>()

    class BaseViewHolder<VB : ViewDataBinding>(val v: VB) : RecyclerView.ViewHolder(v.root)

    abstract fun getLayout(): Int

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        BaseViewHolder<VB>(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                getLayout(),
                parent,
                false
            )
        )

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun resetList(list: ArrayList<T>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }
}