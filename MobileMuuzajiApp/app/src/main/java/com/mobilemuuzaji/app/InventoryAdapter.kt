package com.mobilemuuzaji.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import com.mobilemuuzaji.app.network.models.InventoryItem
import com.mobilemuuzaji.app.TooltipHelper

class InventoryAdapter(
    context: Context,
    private val allItems: List<InventoryItem>,
    private val onEditClick: (InventoryItem, Int) -> Unit,
    private val onSellClick: (InventoryItem, Int) -> Unit
) : ArrayAdapter<InventoryItem>(context, 0, allItems.toMutableList()), Filterable {

    // Allow for filtering for search
    private var filteredItems: List<InventoryItem> = allItems.toList()

    override fun getCount() = filteredItems.size

    override fun getItem(position: Int) = filteredItems[position]

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_inventory, parent, false)

        val item = allItems[position]
        val btnSell = view.findViewById<Button>(R.id.btnSell)
        val btnEdit = view.findViewById<Button>(R.id.btnEdit)

        view.findViewById<TextView>(R.id.tvItemName).text     = item.item_name
        view.findViewById<TextView>(R.id.tvItemQuantity).text = "Qty: ${item.item_quantity} ${item.unit}"
        view.findViewById<TextView>(R.id.tvItemCost).text     = "Cost: ${item.cost_per_unit} per unit"

        btnSell.setOnClickListener {
            onSellClick(item, position)
        }
        btnEdit.setOnClickListener {
            onEditClick(item, position)
        }

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {

            // Runs on a background thread — does the actual filtering
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()

                results.values = if (constraint.isNullOrBlank()) {
                    // No query — return everything
                    allItems.toList()
                } else {
                    val query = constraint.toString().lowercase().trim()
                    allItems.filter { item ->
                        item.item_name.lowercase().contains(query)
                    }
                }

                return results
            }

            // Runs on the main thread — updates the displayed list
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredItems = results?.values as? List<InventoryItem> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}