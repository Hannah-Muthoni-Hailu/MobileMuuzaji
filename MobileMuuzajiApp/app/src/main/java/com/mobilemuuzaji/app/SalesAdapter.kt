package com.mobilemuuzaji.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.mobilemuuzaji.app.network.models.SalesItem

class SalesAdapter(
    context: Context,
    private val allItems: List<SalesItem>
) : ArrayAdapter<SalesItem>(context, 0, allItems.toMutableList()), Filterable {

    private var filteredItems: List<SalesItem> = allItems.toList()

    override fun getCount() = filteredItems.size

    override fun getItem(position: Int) = filteredItems[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_sale, parent, false)

        val item = allItems[position]

        view.findViewById<TextView>(R.id.tvSaleItemName).text  = item.item_name
        view.findViewById<TextView>(R.id.tvSaleQuantity).text  = "Qty: ${item.item_quantity}"
        view.findViewById<TextView>(R.id.tvSaleEarnings).text  = "Earnings: ${item.earnings}"

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()

                results.values = if (constraint.isNullOrBlank()) {
                    allItems.toList()
                } else {
                    val query = constraint.toString().lowercase().trim()
                    allItems.filter { item ->
                        item.item_name.lowercase().contains(query)
                    }
                }

                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredItems = results?.values as? List<SalesItem> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}