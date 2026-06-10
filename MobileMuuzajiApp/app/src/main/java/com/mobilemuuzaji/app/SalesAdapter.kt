package com.mobilemuuzaji.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.mobilemuuzaji.app.network.models.SalesItem

class SalesAdapter(
    context: Context,
    private val items: List<SalesItem>
) : ArrayAdapter<SalesItem>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_sale, parent, false)

        val item = items[position]

        view.findViewById<TextView>(R.id.tvSaleItemName).text  = item.item_name
        view.findViewById<TextView>(R.id.tvSaleQuantity).text  = "Qty: ${item.item_quantity}"
        view.findViewById<TextView>(R.id.tvSaleEarnings).text  = "Earnings: ${item.earnings}"

        return view
    }
}