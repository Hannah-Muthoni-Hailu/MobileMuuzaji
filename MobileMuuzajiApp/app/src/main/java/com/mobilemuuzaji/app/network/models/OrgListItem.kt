package com.mobilemuuzaji.app.network.models

// Represents a single organization in the list
// role is either "Admin" or "Employee"
data class OrgListItem(
    val id: Int,
    val name: String,
    val role: String
)