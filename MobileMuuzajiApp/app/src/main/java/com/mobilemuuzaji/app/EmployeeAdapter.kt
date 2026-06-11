package com.mobilemuuzaji.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import com.mobilemuuzaji.app.network.models.UserData

class EmployeeAdapter(
    context: Context,
    private val employees: List<UserData>,
    private val onRemoveClick: (UserData) -> Unit
) : ArrayAdapter<UserData>(context, 0, employees) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_employee, parent, false)

        val employee = employees[position]

        view.findViewById<TextView>(R.id.tvEmployeeName).text  = employee.name
        view.findViewById<TextView>(R.id.tvEmployeeEmail).text = employee.email

        view.findViewById<Button>(R.id.btnRemoveEmployee).setOnClickListener {
            onRemoveClick(employee)   // TODO: implement remove
        }

        return view
    }
}