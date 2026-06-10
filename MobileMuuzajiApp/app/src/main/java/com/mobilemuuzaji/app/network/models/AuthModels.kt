package com.mobilemuuzaji.app.network.models

// Sent to POST /signup
data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val password_repeat: String
)

// Sent to POST /login
data class LoginRequest(
    val email: String,
    val password: String
)

// Returned by both signup and login on success
data class AuthResponse(
    val message: String,
    val user: UserData
)

data class UserData(
    val id: Int,
    val name: String,
    val email: String,
    val admin_orgs: List<OrganizationData>,
    val employee_orgs: List<OrganizationData>
)

data class OrganizationData(
    val id: Int,
    val org_name: String
)

data class NewOrgRequest(
    val org_name: String,
    val admin_id: Int
)

data class InventoryItem(
    val id:            Int,
    val item_name:     String,
    val item_quantity: Int,
    val unit:          String,
    val cost_per_unit: Int
)

data class SalesItem(
    val id:            Int,
    val item_name:     String,
    val item_quantity: Int,
    val earnings:      Int,
    val date:          String
)

data class OrganizationDetails(
    val id:          Int,
    val name:        String,
    val admin_id:    Int,
    val admin_name:  String,
    val employees:   List<UserData>,
    val inv_items:   List<InventoryItem>,
    val sales_items: List<SalesItem>
)

data class OrganizationDetailsResponse(
    val organization: OrganizationDetails
)

// Returned when validation fails
data class ErrorResponse(
    val detail: Any?  = null,   // Pydantic validation errors
    val message: String?                = null    // general error message
)

data class ValidationError(
    val type: String,
    val loc: List<String>,
    val msg: String,
    val input: String?,         // the value the user actually entered
    val ctx: ValidationContext?  // optional extra context
)

data class ValidationContext(
    val error: Map<String, Any>  // flexible since error contents vary
)