package com.example.serviceandroid.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import com.example.serviceandroid.R


class CustomArrayAdapter(context: Context?, resource: Int, private val objects: List<String?>?) :
    ArrayAdapter<String?>(context!!, resource, objects!!) {

    private var filteredData: List<String?>? = objects

    override fun getCount(): Int {
        return if(filteredData?.isNotEmpty() == true) filteredData!!.size else 0
    }

    override fun getItem(position: Int): String? {
        return filteredData?.get(position)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.layout_auto_text, parent, false)
        }
        val textView = convertView!!.findViewById<TextView>(R.id.textAuto)
        textView.text = getItem(position)
        return convertView
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                if (constraint != null) {
                    val suggestions = objects?.filter { it!!.contains(constraint, true) }
                    filterResults.values = suggestions
                    filterResults.count = suggestions!!.size
                }
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredData = results?.values as List<String>
                notifyDataSetChanged()
            }
        }
    }
}