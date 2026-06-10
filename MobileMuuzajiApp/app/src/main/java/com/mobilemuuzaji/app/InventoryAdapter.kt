package com.mobilemuuzaji.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import com.mobilemuuzaji.app.network.models.InventoryItem

class InventoryAdapter(
    context: Context,
    private val items: List<InventoryItem>
) : ArrayAdapter<InventoryItem>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_inventory, parent, false)

        val item = items[position]

        view.findViewById<TextView>(R.id.tvItemName).text     = item.item_name
        view.findViewById<TextView>(R.id.tvItemQuantity).text = "Qty: ${item.item_quantity} ${item.unit}"
        view.findViewById<TextView>(R.id.tvItemCost).text     = "Cost: ${item.cost_per_unit} per unit"

        // Dummy buttons — functionality added later
        view.findViewById<Button>(R.id.btnSell).setOnClickListener {
            // TODO: implement sell
        }
        view.findViewById<Button>(R.id.btnEdit).setOnClickListener {
            // TODO: implement edit
        }

        return view
    }
}