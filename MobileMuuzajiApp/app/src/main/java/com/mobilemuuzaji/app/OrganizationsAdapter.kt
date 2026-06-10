package com.mobilemuuzaji.app

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.mobilemuuzaji.app.network.models.OrgListItem

class OrganizationsAdapter(
    context: Context,
    private val organizations: List<OrgListItem>
) : ArrayAdapter<OrgListItem>(context, 0, organizations) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_organization, parent, false)

        val org = organizations[position]

        val tvOrgName = view.findViewById<TextView>(R.id.tvOrgName)
        val tvOrgRole = view.findViewById<TextView>(R.id.tvOrgRole)

        tvOrgName.text = org.name
        tvOrgRole.text = org.role

        // Green background for Admin, blue for Employee
        if (org.role == "Admin") {
            tvOrgRole.setBackgroundColor(Color.parseColor("#2E7D32"))
        } else {
            tvOrgRole.setBackgroundColor(Color.parseColor("#1565C0"))
        }

        return view
    }
}