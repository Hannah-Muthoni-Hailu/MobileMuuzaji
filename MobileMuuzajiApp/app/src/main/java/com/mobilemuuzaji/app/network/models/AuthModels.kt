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
    val id:             Int,
    val item_name:      String,
    val item_quantity:  Int,
    val unit:           String,
    val buying_price:   Int,
    val selling_price:  Int,
    val vat_percentage: Int?   = null
)

data class SalesItem(
    val id:           Int,
    val item_name:    String,
    val item_quantity:Int,
    val buying_price: Int,
    val selling_price:Int,
    val gross_income: Int,
    val profit:       Int,
    val vat_amount:   Int?   = null,
    val date:         String
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

data class NewInventoryRequest(
    val name:           String,
    val quantity:       Int,
    val unit:           String,
    val buying_price:   Int,
    val selling_price:  Int,
    val vat_percentage: Int?   = null,
    val org_id:         Int
)

data class InventoryItemResponse(
    val id:            Int,
    val item_name:     String,
    val item_quantity: Int,
    val unit:          String,
    val buying_price:   Int,
    val selling_price:  Int,
    val vat_percentage: Int?   = null,
    val org_id:        Int
)

data class UpdateInventoryRequest(
    val item_name:      String,
    val item_quantity:  Int,
    val unit:           String,
    val buying_price:   Int,
    val selling_price:  Int,
    val vat_percentage: Int?   = null,
    val org_id:         Int
)

data class SaleRequest(
    val item_id: Int,
    val quantity_sold: Int,
    val sale_price: Int?    = null,
    val update_price: Boolean = false
)

data class SaleResponse(
    val id:            Int,
    val item_name:     String,
    val item_quantity: Int,
    val buying_price:  Int,           // ← new
    val selling_price: Int,           // ← new
    val gross_income:  Int,           // ← new
    val profit:        Int,           // ← new
    val vat_amount:    Int?   = null, // ← new
    val date:          String,
    val org_id:        Int
)

data class GroupedSaleItem(
    val item_name:       String,
    val total_quantity:  Int,
    val total_gross:     Int,
    val total_profit:    Int,
    val total_vat:       Int,
    val sale_count:      Int
)

data class NewEmployeeRequest(
    val org_id:         Int,
    val employee_email: String
)

data class EmployeeOrgResponse(
    val id:         Int,
    val name:       String,
    val admin_id:   Int,
    val admin_name: String,
    val employees:  List<EmployeeData>
)

data class EmployeeData(
    val id:    Int,
    val name:  String,
    val email: String
) : java.io.Serializable

data class RemoveEmployeeRequest(
    val org_id:      Int,
    val employee_id: Int
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