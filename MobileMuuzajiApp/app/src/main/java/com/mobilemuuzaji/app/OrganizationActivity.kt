package com.mobilemuuzaji.app

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mobilemuuzaji.app.database.AppDatabase
import com.mobilemuuzaji.app.database.entities.InventoryEntity
import com.mobilemuuzaji.app.database.entities.SalesEntity
import com.mobilemuuzaji.app.network.ApiClient
import com.mobilemuuzaji.app.network.models.InventoryItem
import com.mobilemuuzaji.app.network.models.SalesItem
import com.mobilemuuzaji.app.network.models.UpdateInventoryRequest
import com.mobilemuuzaji.app.network.models.GroupedSaleItem
import com.mobilemuuzaji.app.network.models.EmployeeData
import java.util.Calendar
import android.widget.RadioButton
import android.widget.RadioGroup
import com.mobilemuuzaji.app.network.models.SaleRequest
import com.mobilemuuzaji.app.network.models.SaleResponse
import com.mobilemuuzaji.app.repository.InventoryRepository
import com.mobilemuuzaji.app.repository.SalesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import com.mobilemuuzaji.app.database.entities.UserEntity
import com.mobilemuuzaji.app.database.entities.OrganizationEntity
import com.mobilemuuzaji.app.network.models.OrganizationDetails
import com.mobilemuuzaji.app.repository.OrganizationRepository
import com.mobilemuuzaji.app.repository.UserRepository
import android.widget.EditText
import android.widget.ImageButton
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.app.Dialog
import android.view.Window
import android.content.Intent
import com.mobilemuuzaji.app.network.models.NewInventoryRequest
import com.google.gson.Gson
import com.mobilemuuzaji.app.network.models.ErrorResponse
import com.mobilemuuzaji.app.network.models.UserData

data class SalesFilterState(
    val dateFilter:  String?  = null,   // "today", "week", "month", "all", or custom range
    val sortBy:      String?  = null,   // "date", "alphabetical", "earnings"
    val customStart: Long?    = null,
    val customEnd:   Long?    = null
)

class OrganizationActivity : AppCompatActivity() {

    private lateinit var tvOrgName:      TextView
    private lateinit var tvAdminName:    TextView
    private lateinit var btnNewInventory:Button
    private lateinit var progressBar:    ProgressBar
    private lateinit var tvError:        TextView
    private lateinit var tabInventory:   TextView
    private lateinit var tabSales:       TextView
    private lateinit var tabIndicator:   View
    private lateinit var tvEmptyState:   TextView
    private lateinit var lvItems:        ListView
    private lateinit var btnSearch:     ImageButton
    private lateinit var llSearchBar:   LinearLayout
    private lateinit var etSearch:      EditText
    private lateinit var btnClearSearch:ImageButton
    private lateinit var btnOpenPanel:              ImageButton
    private lateinit var llSidePanel:               LinearLayout
    private lateinit var btnClosePanel:             ImageButton
    private lateinit var tvWelcome:                 TextView
    private lateinit var panelTabInventory:         TextView
    private lateinit var panelTabEmployees:         TextView
    private lateinit var llPanelInventoryContent:   LinearLayout
    private lateinit var llPanelEmployeeContent:    LinearLayout
    private lateinit var btnNewEmployee:            Button
    private lateinit var lvEmployees:               ListView
    private lateinit var sessionManager:   SessionManager

    private var isSearchVisible = false

    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var salesRepository:     SalesRepository
    private lateinit var organizationRepository: OrganizationRepository
    private val db by lazy { AppDatabase.getInstance(applicationContext) }

    private lateinit var btnFilter:       ImageButton
    private lateinit var btnGroupToggle:  ImageButton
    private lateinit var llActiveFilters: LinearLayout
    private lateinit var tvActiveFilter:  TextView
    private lateinit var tvClearFilter:   TextView

    private var isGroupedView   = false
    private var filterState     = SalesFilterState()
    private var groupedSales    = listOf<GroupedSaleItem>()

    private var isPanelOpen    = false
    private var isAdminOfOrg   = false
    private var employees      = listOf<UserData>()

    private var orgId   = -1
    private var orgName = ""

    // Current tab — true = inventory, false = sales
    private var showingInventory = true

    // Data lists
    private var inventoryItems = listOf<InventoryItem>()
    private var salesItems     = listOf<SalesItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_organization)

        sessionManager = SessionManager(this)

        // Retrieve data passed from OrganizationsActivity
        orgId   = intent.getIntExtra("org_id", -1)
        orgName = intent.getStringExtra("org_name") ?: "Organization"

        // Bind views
        tvOrgName       = findViewById(R.id.tvOrgName)
        tvAdminName     = findViewById(R.id.tvAdminName)
        btnNewInventory = findViewById(R.id.btnNewInventory)
        progressBar     = findViewById(R.id.progressBar)
        tvError         = findViewById(R.id.tvError)
        tabInventory    = findViewById(R.id.tabInventory)
        tabSales        = findViewById(R.id.tabSales)
        tabIndicator    = findViewById(R.id.tabIndicator)
        tvEmptyState    = findViewById(R.id.tvEmptyState)
        lvItems         = findViewById(R.id.lvItems)
        btnSearch      = findViewById(R.id.btnSearch)
        llSearchBar    = findViewById(R.id.llSearchBar)
        etSearch       = findViewById(R.id.etSearch)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        btnFilter       = findViewById(R.id.btnFilter)
        btnGroupToggle  = findViewById(R.id.btnGroupToggle)
        llActiveFilters = findViewById(R.id.llActiveFilters)
        tvActiveFilter  = findViewById(R.id.tvActiveFilter)
        tvClearFilter   = findViewById(R.id.tvClearFilter)

        btnFilter.setOnClickListener {
            showFilterDropdown()
        }

        tvOrgName.text = orgName

        // Set up repositories
        val db = AppDatabase.getInstance(applicationContext)
        inventoryRepository = InventoryRepository(db.inventoryDao())
        salesRepository     = SalesRepository(db.salesDao())
        organizationRepository = OrganizationRepository(db.organizationDao())

        // Tab click listeners
        tabInventory.setOnClickListener { switchTab(true) }
        tabSales.setOnClickListener     { switchTab(false) }

        // Dummy button
        btnNewInventory.setOnClickListener {
            showNewInventoryDialog()
        }

        // Toggle search bar visibility when search icon is tapped
        btnSearch.setOnClickListener {
            isSearchVisible = !isSearchVisible
            if (isSearchVisible) {
                llSearchBar.visibility = View.VISIBLE
                etSearch.requestFocus()
                // Show keyboard automatically
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
            } else {
                llSearchBar.visibility = View.GONE
                etSearch.text.clear()
                refreshList()   // reset to full list
                // Hide keyboard
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
            }
        }

        // Filtering toggles
        btnGroupToggle.setOnClickListener {
            isGroupedView = !isGroupedView
            // Change icon to visually indicate current mode
            btnGroupToggle.setImageResource(
                if (isGroupedView) android.R.drawable.ic_menu_my_calendar
                else android.R.drawable.ic_menu_agenda
            )
            refreshSalesView()
        }

        tvClearFilter.setOnClickListener {
            filterState = SalesFilterState()
            llActiveFilters.visibility = View.GONE
            refreshSalesView()
        }

        // Clear button wipes the search and resets the list
        btnClearSearch.setOnClickListener {
            etSearch.text.clear()
            llSearchBar.visibility = View.GONE
            isSearchVisible = false
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
            refreshList()
        }

        // Filter as the user types
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                // Get the current adapter and call filter on it
                when (val adapter = lvItems.adapter) {
                    is InventoryAdapter -> adapter.filter.filter(query)
                    is SalesAdapter     -> adapter.filter.filter(query)
                    is GroupedSalesAdapter  -> adapter.filter.filter(query)
                }
            }
        })

        // Load from local Room database first
        loadFromLocal()

        // If online, fetch fresh data from API and update Room
        if (NetworkUtils.isOnline(this)) {
            fetchFromApi()
        }

        setupSidePanel()
    }

    private fun loadFromLocal() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val localInventory = inventoryRepository.getInventoryForOrganization(orgId)
                val localSales     = salesRepository.getSalesForOrganization(orgId)

                // Convert Room entities to display models
                inventoryItems = localInventory.map { entity ->
                    InventoryItem(
                        id            = entity.id,
                        item_name     = entity.itemName,
                        item_quantity = entity.itemQuantity,
                        unit          = entity.unit,
                        cost_per_unit = entity.costPerUnit
                    )
                }

                salesItems = localSales.map { entity ->
                    SalesItem(
                        id            = entity.id,
                        item_name     = entity.itemName,
                        item_quantity = entity.itemQuantity,
                        earnings      = entity.earnings,
                        date          = entity.date.toString()
                    )
                }
            }

            // Update UI on main thread
            updateList()
        }
    }

    private suspend fun saveOrganizationToRoom(org: OrganizationDetails) {
        val userRepo = UserRepository(db.userDao())
        val existingUser = userRepo.getUserById(org.id)

        if (existingUser == null) {
            userRepo.saveUser(
                UserEntity(
                    id       = org.id,
                    name     = org.admin_name,
                    email    = "",
                    password = "",
                    isSynced = true
                )
            )
        }

        organizationRepository.saveOrganization(
            OrganizationEntity(
                id       = org.id,
                orgName  = org.name,
                adminId  = org.admin_id,
                isSynced = true
            )
        )

        // Save employees into Room and create junction entries so
        // local queries (getEmployeesForOrganization) return them.
        org.employees.forEach { employee ->
            if (userRepo.getUserById(employee.id) == null) {
                userRepo.saveUser(
                    UserEntity(
                        id       = employee.id,
                        name     = employee.name,
                        email    = employee.email,
                        password = "",
                        isSynced = true
                    )
                )
            }
            organizationRepository.addEmployee(
                employeeId     = employee.id,
                organizationId = org.id
            )
        }
    }

    private fun fetchFromApi() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            tvError.visibility     = View.GONE

            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.apiService.getOrganization(orgId)
                }

                Log.d("OrgActivity", "Response code: ${response.code()}")
                Log.d("OrgActivity", "Response body: ${response.body()}")
                Log.d("OrgActivity", "Error body: ${response.errorBody()?.string()}")

                if (response.isSuccessful) {
                    val org = response.body()!!.organization

                    Log.d("OrgActivity", "Org name: ${org.name}")
                    Log.d("OrgActivity", "Inventory count: ${org.inv_items.size}")
                    Log.d("OrgActivity", "Sales count: ${org.sales_items.size}")

                    tvAdminName.text = "Admin: ${org.admin_name}"

                    // Update display lists from API response
                    inventoryItems = org.inv_items
                    salesItems     = org.sales_items

                    // Save to Room for offline use
                    withContext(Dispatchers.IO) {
                        saveOrganizationToRoom(org)
                        saveInventoryToRoom(org.inv_items)
                        saveSalesToRoom(org.sales_items)
                    }

                    updateList()

                } else {
                    Log.e("OrgActivity", "API error: ${response.errorBody()?.string()}")
                    tvError.text       = "Could not refresh data"
                    tvError.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Log.e("OrgActivity", "Exception: ${e.message}")
                Log.e("OrgActivity", "Stack trace: ${e.stackTraceToString()}")
                tvError.text       = "Network error: ${e.message}"
                tvError.visibility = View.VISIBLE
            }

            progressBar.visibility = View.GONE
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody == null) return "An unexpected error occurred"
        return try {
            val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)
            when (val detail = errorResponse.detail) {
                is String   -> detail
                is List<*>  -> {
                    val map  = (detail.firstOrNull()) as? Map<*, *>
                    val loc  = (map?.get("loc") as? List<*>)?.lastOrNull() ?: "field"
                    val msg  = map?.get("msg") ?: "Unknown error"
                    "$loc: $msg"
                }
                else -> "An unexpected error occurred"
            }
        } catch (e: Exception) {
            "An unexpected error occurred"
        }
    }

    private suspend fun saveInventoryToRoom(items: List<InventoryItem>) {
        items.forEach { item ->
            inventoryRepository.saveInventoryItem(
                InventoryEntity(
                    id           = item.id,
                    itemName     = item.item_name,
                    itemQuantity = item.item_quantity,
                    unit         = item.unit,
                    costPerUnit  = item.cost_per_unit,
                    orgId        = orgId,
                    isSynced     = true    // came from server so already synced
                )
            )
        }
    }

    private suspend fun saveSalesToRoom(items: List<SalesItem>) {
        items.forEach { item ->
            salesRepository.saveSale(
                SalesEntity(
                    id           = item.id,
                    itemName     = item.item_name,
                    itemQuantity = item.item_quantity,
                    earnings     = item.earnings,
                    orgId        = orgId,
                    isSynced     = true
                )
            )
        }
    }

    private fun switchTab(toInventory: Boolean) {
        showingInventory = toInventory

        // Show filter and group buttons only on sales tab
        btnFilter.visibility      = if (toInventory) View.GONE else View.VISIBLE
        btnGroupToggle.visibility = if (toInventory) View.GONE else View.VISIBLE

        if (toInventory) {
            tabInventory.setTextColor(Color.parseColor("#1976D2"))
            tabInventory.setTypeface(null, android.graphics.Typeface.BOLD)
            tabSales.setTextColor(Color.parseColor("#888888"))
            tabSales.setTypeface(null, android.graphics.Typeface.NORMAL)
            llActiveFilters.visibility = View.GONE
        } else {
            tabSales.setTextColor(Color.parseColor("#1976D2"))
            tabSales.setTypeface(null, android.graphics.Typeface.BOLD)
            tabInventory.setTextColor(Color.parseColor("#888888"))
            tabInventory.setTypeface(null, android.graphics.Typeface.NORMAL)
            updateActiveFilterLabel()
        }

        refreshList()   // ← call refreshList instead of updateList
    }

    private fun refreshList() {
        // Clear search when switching tabs
        etSearch.text.clear()
        
        if (showingInventory) {
            lvItems.adapter = InventoryAdapter(
                context  = this,
                allItems = inventoryItems,
                onEditClick = { item, position ->
                    showEditInventoryDialog(item, position)
                },
                onSellClick = { item, position ->
                    showSellDialog(item, position)
                }
            )
            tvEmptyState.text       = "No inventory items"
            tvEmptyState.visibility = if (inventoryItems.isEmpty()) View.VISIBLE else View.GONE
        } else {
            refreshSalesView()
        }
    }

    private fun updateList() {
        refreshList()                  // update the list
        switchTab(showingInventory)    // update the tab styles
    }

    companion object {
        // These match exactly the string values pint generates in your Python backend
        val UNIT_OPTIONS = listOf(
            "kilogram",
            "gram",
            "pound",
            "ounce",
            "metric_ton",
            "liter",
            "milliliter",
            "gallon",
            "fluid_ounce",
            "cup"
        )
    }

    private fun showNewInventoryDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_new_inventory)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val llErrors        = dialog.findViewById<LinearLayout>(R.id.llInventoryDialogErrors)
        val etName          = dialog.findViewById<EditText>(R.id.etInventoryName)
        val etQuantity      = dialog.findViewById<EditText>(R.id.etInventoryQuantity)
        val spinnerUnit     = dialog.findViewById<Spinner>(R.id.spinnerUnit)
        val etCost          = dialog.findViewById<EditText>(R.id.etInventoryCost)
        val btnCancel       = dialog.findViewById<Button>(R.id.btnInventoryDialogCancel)
        val btnSubmit       = dialog.findViewById<Button>(R.id.btnInventoryDialogSubmit)

        // Populate the unit spinner
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            UNIT_OPTIONS
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUnit.adapter = spinnerAdapter

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            val name     = etName.text.toString().trim()
            val quantity = etQuantity.text.toString().trim()
            val unit     = spinnerUnit.selectedItem.toString()
            val cost     = etCost.text.toString().trim()

            // Client side validation
            val errors = mutableListOf<String>()
            if (name.isEmpty())     errors.add("Item name is required")
            if (quantity.isEmpty()) errors.add("Quantity is required")
            if (cost.isEmpty())     errors.add("Cost per unit is required")

            if (errors.isNotEmpty()) {
                showDialogErrors(llErrors, errors)
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false

            val newItem = NewInventoryRequest(
                name          = name,
                quantity      = quantity.toInt(),
                unit          = unit,
                cost_per_unit = cost.toInt(),
                org_id        = orgId
            )

            lifecycleScope.launch {
                if (NetworkUtils.isOnline(this@OrganizationActivity)) {
                    // Online — save to backend first, then Room
                    try {
                        val response = withContext(Dispatchers.IO) {
                            ApiClient.apiService.createInventoryItem(newItem)
                        }

                        if (response.isSuccessful) {
                            val createdItem = response.body()!!

                            // Save to Room with server-assigned id
                            withContext(Dispatchers.IO) {
                                inventoryRepository.saveInventoryItem(
                                    InventoryEntity(
                                        id           = createdItem.id,
                                        itemName     = createdItem.item_name,
                                        itemQuantity = createdItem.item_quantity,
                                        unit         = createdItem.unit,
                                        costPerUnit  = createdItem.cost_per_unit,
                                        orgId        = orgId,
                                        isSynced     = true
                                    )
                                )
                            }

                            // Add to the in-memory list and refresh UI
                            val updatedList = inventoryItems.toMutableList()
                            updatedList.add(
                                InventoryItem(
                                    id            = createdItem.id,
                                    item_name     = createdItem.item_name,
                                    item_quantity = createdItem.item_quantity,
                                    unit          = createdItem.unit,
                                    cost_per_unit = createdItem.cost_per_unit
                                )
                            )
                            inventoryItems = updatedList
                            refreshList()
                            dialog.dismiss()

                        } else {
                            val errorBody = response.errorBody()?.string()
                            showDialogErrors(llErrors, listOf(parseErrorMessage(errorBody)))
                        }

                    } catch (e: Exception) {
                        showDialogErrors(llErrors, listOf("Network error: ${e.message}"))
                    }

                } else {
                    // Offline — save to Room only with a temporary negative id
                    // Negative ids signal unsynced local records
                    val tempId = -(System.currentTimeMillis().toInt())

                    withContext(Dispatchers.IO) {
                        inventoryRepository.saveInventoryItem(
                            InventoryEntity(
                                id           = tempId,
                                itemName     = name,
                                itemQuantity = quantity.toInt(),
                                unit         = unit,
                                costPerUnit  = cost.toInt(),
                                orgId        = orgId,
                                isSynced     = false    // will be synced when online
                            )
                        )
                    }

                    // Add to in-memory list and refresh UI
                    val updatedList = inventoryItems.toMutableList()
                    updatedList.add(
                        InventoryItem(
                            id            = tempId,
                            item_name     = name,
                            item_quantity = quantity.toInt(),
                            unit          = unit,
                            cost_per_unit = cost.toInt()
                        )
                    )
                    inventoryItems = updatedList
                    refreshList()
                    dialog.dismiss()
                }

                btnSubmit.isEnabled = true
            }
        }

        dialog.show()
    }

    private fun showDialogErrors(container: LinearLayout, errors: List<String>) {
        container.removeAllViews()
        errors.forEach { message ->
            val errorView = layoutInflater.inflate(R.layout.item_error, container, false)
            errorView.findViewById<TextView>(R.id.tvError).text = message
            container.addView(errorView)
        }
        container.visibility = View.VISIBLE
    }

    private fun showEditInventoryDialog(item: InventoryItem, position: Int) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_inventory)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val llErrors    = dialog.findViewById<LinearLayout>(R.id.llEditInventoryDialogErrors)
        val etName      = dialog.findViewById<EditText>(R.id.etEditInventoryName)
        val etQuantity  = dialog.findViewById<EditText>(R.id.etEditInventoryQuantity)
        val spinnerUnit = dialog.findViewById<Spinner>(R.id.spinnerEditUnit)
        val etCost      = dialog.findViewById<EditText>(R.id.etEditInventoryCost)
        val btnCancel   = dialog.findViewById<Button>(R.id.btnEditInventoryDialogCancel)
        val btnSubmit   = dialog.findViewById<Button>(R.id.btnEditInventoryDialogSubmit)

        // Populate the unit spinner
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            UNIT_OPTIONS
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUnit.adapter = spinnerAdapter

        // Pre-populate fields with existing item data
        etName.setText(item.item_name)
        etQuantity.setText(item.item_quantity.toString())
        etCost.setText(item.cost_per_unit.toString())

        // Pre-select the current unit in the spinner
        val unitIndex = UNIT_OPTIONS.indexOf(item.unit)
        if (unitIndex >= 0) spinnerUnit.setSelection(unitIndex)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            val name     = etName.text.toString().trim()
            val quantity = etQuantity.text.toString().trim()
            val unit     = spinnerUnit.selectedItem.toString()
            val cost     = etCost.text.toString().trim()

            // Validation
            val errors = mutableListOf<String>()
            if (name.isEmpty())     errors.add("Item name is required")
            if (quantity.isEmpty()) errors.add("Quantity is required")
            if (cost.isEmpty())     errors.add("Cost per unit is required")

            if (errors.isNotEmpty()) {
                showDialogErrors(llErrors, errors)
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false

            lifecycleScope.launch {
                if (NetworkUtils.isOnline(this@OrganizationActivity)) {
                    // Online — update backend first then Room
                    try {
                        val response = withContext(Dispatchers.IO) {
                            ApiClient.apiService.updateInventoryItem(
                                itemId  = item.id,
                                request = UpdateInventoryRequest(
                                    item_name     = name,
                                    item_quantity = quantity.toInt(),
                                    unit          = unit,
                                    cost_per_unit = cost.toInt(),
                                    org_id        = orgId
                                )
                            )
                        }

                        if (response.isSuccessful) {
                            val updatedItem = response.body()!!

                            // Update Room
                            withContext(Dispatchers.IO) {
                                inventoryRepository.updateInventoryItem(
                                    InventoryEntity(
                                        id           = updatedItem.id,
                                        itemName     = updatedItem.item_name,
                                        itemQuantity = updatedItem.item_quantity,
                                        unit         = updatedItem.unit,
                                        costPerUnit  = updatedItem.cost_per_unit,
                                        orgId        = orgId,
                                        isSynced     = true
                                    )
                                )
                            }

                            // Update in-memory list
                            updateInventoryItemInList(
                                position = position,
                                id            = updatedItem.id,
                                item_name     = updatedItem.item_name,
                                item_quantity = updatedItem.item_quantity,
                                unit          = updatedItem.unit,
                                cost_per_unit = updatedItem.cost_per_unit
                            )

                            dialog.dismiss()

                        } else {
                            val errorBody = response.errorBody()?.string()
                            showDialogErrors(llErrors, listOf(parseErrorMessage(errorBody)))
                        }

                    } catch (e: Exception) {
                        showDialogErrors(llErrors, listOf("Network error: ${e.message}"))
                    }

                } else {
                    // Offline — update Room only, mark as unsynced
                    withContext(Dispatchers.IO) {
                        inventoryRepository.updateInventoryItem(
                            InventoryEntity(
                                id           = item.id,
                                itemName     = name,
                                itemQuantity = quantity.toInt(),
                                unit         = unit,
                                costPerUnit  = cost.toInt(),
                                orgId        = orgId,
                                isSynced     = false    // will sync when online
                            )
                        )
                    }

                    // Update in-memory list
                    updateInventoryItemInList(
                        position      = position,
                        id            = item.id,
                        item_name     = name,
                        item_quantity = quantity.toInt(),
                        unit          = unit,
                        cost_per_unit = cost.toInt()
                    )

                    dialog.dismiss()
                }

                btnSubmit.isEnabled = true
            }
        }

        dialog.show()
    }

    // Helper to update a single item in the in-memory list and refresh the UI
    private fun updateInventoryItemInList(
        position:      Int,
        id:            Int,
        item_name:     String,
        item_quantity: Int,
        unit:          String,
        cost_per_unit: Int
    ) {
        val updatedList = inventoryItems.toMutableList()
        updatedList[position] = InventoryItem(
            id            = id,
            item_name     = item_name,
            item_quantity = item_quantity,
            unit          = unit,
            cost_per_unit = cost_per_unit
        )
        inventoryItems = updatedList
        refreshList()
    }

    private fun showSellDialog(item: InventoryItem, position: Int) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_sell)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val tvSellTitle      = dialog.findViewById<TextView>(R.id.tvSellTitle)
        val tvCurrentStock   = dialog.findViewById<TextView>(R.id.tvCurrentStock)
        val llErrors         = dialog.findViewById<LinearLayout>(R.id.llSellDialogErrors)
        val rgSellMode       = dialog.findViewById<RadioGroup>(R.id.rgSellMode)
        val rbQuantitySold   = dialog.findViewById<RadioButton>(R.id.rbQuantitySold)
        val rbLeftover       = dialog.findViewById<RadioButton>(R.id.rbLeftover)
        val tvQuantityLabel  = dialog.findViewById<TextView>(R.id.tvQuantityLabel)
        val etSellQuantity   = dialog.findViewById<EditText>(R.id.etSellQuantity)
        val tvCalculatedSold = dialog.findViewById<TextView>(R.id.tvCalculatedSold)
        val btnCancel        = dialog.findViewById<Button>(R.id.btnSellDialogCancel)
        val btnSubmit        = dialog.findViewById<Button>(R.id.btnSellDialogSubmit)

        // Populate title and stock info
        tvSellTitle.text    = "Sell ${item.item_name}"
        tvCurrentStock.text = "Current stock: ${item.item_quantity} ${item.unit}"

        // Switch label and show calculated result when radio changes
        rgSellMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbQuantitySold -> {
                    tvQuantityLabel.text       = "Quantity Sold"
                    tvCalculatedSold.visibility = View.GONE
                    etSellQuantity.text.clear()
                    etSellQuantity.hint = "Enter quantity sold"
                }
                R.id.rbLeftover -> {
                    tvQuantityLabel.text       = "Leftover Quantity"
                    etSellQuantity.text.clear()
                    etSellQuantity.hint = "Enter leftover quantity"
                }
            }
        }

        // Show calculated quantity sold as user types in leftover mode
        etSellQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (rbLeftover.isChecked) {
                    val leftover = s.toString().toIntOrNull()
                    if (leftover != null) {
                        val quantitySold = item.item_quantity - leftover
                        if (quantitySold > 0) {
                            tvCalculatedSold.text       = "Quantity sold: $quantitySold"
                            tvCalculatedSold.visibility = View.VISIBLE
                        } else {
                            tvCalculatedSold.visibility = View.GONE
                        }
                    } else {
                        tvCalculatedSold.visibility = View.GONE
                    }
                }
            }
        })

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            val input = etSellQuantity.text.toString().trim()
            llErrors.removeAllViews()
            llErrors.visibility = View.GONE

            // Validate input
            if (input.isEmpty()) {
                showDialogErrors(llErrors, listOf("Please enter a quantity"))
                return@setOnClickListener
            }

            val inputValue = input.toIntOrNull()
            if (inputValue == null || inputValue < 0) {
                showDialogErrors(llErrors, listOf("Please enter a valid number"))
                return@setOnClickListener
            }

            // Calculate quantity sold based on mode
            val quantitySold = if (rbQuantitySold.isChecked) {
                inputValue
            } else {
                item.item_quantity - inputValue   // leftover mode
            }

            // Validate quantity sold
            when {
                quantitySold <= 0 -> {
                    showDialogErrors(llErrors, listOf("Quantity sold must be greater than zero"))
                    return@setOnClickListener
                }
                quantitySold > item.item_quantity -> {
                    showDialogErrors(llErrors, listOf(
                        "Cannot sell more than current stock (${item.item_quantity} ${item.unit})"
                    ))
                    return@setOnClickListener
                }
            }

            btnSubmit.isEnabled = false

            lifecycleScope.launch {
                // Calculate new inventory quantity after sale
                val newQuantity = item.item_quantity - quantitySold

                if (NetworkUtils.isOnline(this@OrganizationActivity)) {
                    // Online — send to backend
                    try {
                        val response = withContext(Dispatchers.IO) {
                            ApiClient.apiService.makeSale(
                                SaleRequest(
                                    item_id       = item.id,
                                    quantity_sold = quantitySold
                                )
                            )
                        }

                        if (response.isSuccessful) {
                            val saleResponse = response.body()!!

                            withContext(Dispatchers.IO) {
                                // Update inventory quantity in Room
                                inventoryRepository.updateInventoryItem(
                                    InventoryEntity(
                                        id           = item.id,
                                        itemName     = item.item_name,
                                        itemQuantity = newQuantity,
                                        unit         = item.unit,
                                        costPerUnit  = item.cost_per_unit,
                                        orgId        = orgId,
                                        isSynced     = true
                                    )
                                )

                                // Save sale record to Room
                                salesRepository.saveSale(
                                    SalesEntity(
                                        id           = saleResponse.id,
                                        itemName     = saleResponse.item_name,
                                        itemQuantity = saleResponse.item_quantity,
                                        earnings     = saleResponse.earnings,
                                        orgId        = orgId,
                                        isSynced     = true
                                    )
                                )
                            }

                            // Update both in-memory lists
                            updateInventoryQuantityInList(position, newQuantity)
                            addSaleToList(saleResponse)
                            dialog.dismiss()

                        } else {
                            val errorBody = response.errorBody()?.string()
                            showDialogErrors(llErrors, listOf(parseErrorMessage(errorBody)))
                        }

                    } catch (e: Exception) {
                        showDialogErrors(llErrors, listOf("Network error: ${e.message}"))
                    }

                } else {
                    // Offline — update Room only
                    val tempSaleId = -(System.currentTimeMillis().toInt())

                    withContext(Dispatchers.IO) {
                        // Update inventory quantity in Room
                        inventoryRepository.updateInventoryItem(
                            InventoryEntity(
                                id           = item.id,
                                itemName     = item.item_name,
                                itemQuantity = newQuantity,
                                unit         = item.unit,
                                costPerUnit  = item.cost_per_unit,
                                orgId        = orgId,
                                isSynced     = false
                            )
                        )

                        // Save sale with temp id
                        salesRepository.saveSale(
                            SalesEntity(
                                id           = tempSaleId,
                                itemName     = item.item_name,
                                itemQuantity = quantitySold,
                                earnings     = 0,      // unknown until synced with backend
                                orgId        = orgId,
                                isSynced     = false
                            )
                        )
                    }

                    // Update both in-memory lists
                    updateInventoryQuantityInList(position, newQuantity)
                    addOfflineSaleToList(item, quantitySold, tempSaleId)
                    dialog.dismiss()
                }

                btnSubmit.isEnabled = true
            }
        }

        dialog.show()
    }

    // Update inventory quantity in the in-memory list
    private fun updateInventoryQuantityInList(position: Int, newQuantity: Int) {
        val updatedList = inventoryItems.toMutableList()
        val existingItem = updatedList[position]
        updatedList[position] = existingItem.copy(item_quantity = newQuantity)
        inventoryItems = updatedList
        refreshList()
    }

    // Add a synced sale from server response to the in-memory sales list
    private fun addSaleToList(saleResponse: SaleResponse) {
        val updatedSales = salesItems.toMutableList()
        updatedSales.add(0,   // add to top since it's the most recent
            SalesItem(
                id            = saleResponse.id,
                item_name     = saleResponse.item_name,
                item_quantity = saleResponse.item_quantity,
                earnings      = saleResponse.earnings,
                date          = saleResponse.date
            )
        )
        salesItems = updatedSales
    }

    // Add an offline sale to the in-memory sales list
    private fun addOfflineSaleToList(item: InventoryItem, quantitySold: Int, tempId: Int) {
        val updatedSales = salesItems.toMutableList()
        updatedSales.add(0,
            SalesItem(
                id            = tempId,
                item_name     = item.item_name,
                item_quantity = quantitySold,
                earnings      = 0,       // unknown until synced
                date          = "Pending sync"
            )
        )
        salesItems = updatedSales
    }

    private fun showFilterDropdown() {
        val options = arrayOf(
            "Sort: Newest first",
            "Sort: Alphabetical",
            "Sort: Earnings (highest first)",
            "─────────────────",
            "Date: Today",
            "Date: This week",
            "Date: This month",
            "Date: All time",
            "Date: Custom range"
        )

        val popup = android.widget.PopupMenu(this, btnFilter)
        options.forEachIndexed { index, option ->
            popup.menu.add(0, index, index, option)
        }

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                0 -> applyFilter(filterState.copy(sortBy = "date"))
                1 -> applyFilter(filterState.copy(sortBy = "alphabetical"))
                2 -> applyFilter(filterState.copy(sortBy = "earnings"))
                3 -> false  // separator — do nothing
                4 -> applyFilter(filterState.copy(dateFilter = "today"))
                5 -> applyFilter(filterState.copy(dateFilter = "week"))
                6 -> applyFilter(filterState.copy(dateFilter = "month"))
                7 -> applyFilter(filterState.copy(dateFilter = "all"))
                8 -> showDateRangePicker()
                else -> false
            }
            true
        }

        popup.show()
    }

    private fun showDateRangePicker() {
        // Start date picker
        val startCalendar = Calendar.getInstance()
        android.app.DatePickerDialog(
            this,
            { _, startYear, startMonth, startDay ->
                val start = Calendar.getInstance()
                start.set(startYear, startMonth, startDay, 0, 0, 0)

                // End date picker
                android.app.DatePickerDialog(
                    this,
                    { _, endYear, endMonth, endDay ->
                        val end = Calendar.getInstance()
                        end.set(endYear, endMonth, endDay, 23, 59, 59)

                        applyFilter(
                            filterState.copy(
                                dateFilter  = "custom",
                                customStart = start.timeInMillis,
                                customEnd   = end.timeInMillis
                            )
                        )
                    },
                    startCalendar.get(Calendar.YEAR),
                    startCalendar.get(Calendar.MONTH),
                    startCalendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            startCalendar.get(Calendar.YEAR),
            startCalendar.get(Calendar.MONTH),
            startCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun applyFilter(newState: SalesFilterState): Boolean {
        filterState = newState
        refreshSalesView()
        updateActiveFilterLabel()
        return true
    }

    private fun updateActiveFilterLabel() {
        val parts = mutableListOf<String>()

        when (filterState.sortBy) {
            "alphabetical" -> parts.add("A-Z")
            "earnings"     -> parts.add("Earnings ↓")
            "date"         -> parts.add("Newest first")
        }

        when (filterState.dateFilter) {
            "today"  -> parts.add("Today")
            "week"   -> parts.add("This week")
            "month"  -> parts.add("This month")
            "custom" -> parts.add("Custom range")
        }

        if (parts.isNotEmpty()) {
            tvActiveFilter.text       = "Filters: ${parts.joinToString(" · ")}"
            llActiveFilters.visibility = View.VISIBLE
        } else {
            llActiveFilters.visibility = View.GONE
        }
    }

    private fun refreshSalesView() {
        val filteredSales = SalesFilterHelper.filter(salesItems, filterState)
        groupedSales      = SalesFilterHelper.group(filteredSales)

        if (isGroupedView) {
            lvItems.adapter         = GroupedSalesAdapter(this, groupedSales)
            tvEmptyState.text       = "No sales recorded"
            tvEmptyState.visibility = if (groupedSales.isEmpty()) View.VISIBLE else View.GONE
        } else {
            lvItems.adapter         = SalesAdapter(this, filteredSales)
            tvEmptyState.text       = "No sales recorded"
            tvEmptyState.visibility = if (filteredSales.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupSidePanel() {
        btnOpenPanel            = findViewById(R.id.btnOpenPanel)
        llSidePanel             = findViewById(R.id.llSidePanel)
        btnClosePanel           = findViewById(R.id.btnClosePanel)
        tvWelcome               = findViewById(R.id.tvWelcome)
        panelTabInventory       = findViewById(R.id.panelTabInventory)
        panelTabEmployees       = findViewById(R.id.panelTabEmployees)

        val userName = sessionManager.getName() ?: "User"
        tvWelcome.text = "Welcome, $userName"

        isAdminOfOrg = sessionManager.getAdminOrgs().any { it.id == orgId }

        // Style employees tab based on admin status
        if (isAdminOfOrg) {
            panelTabEmployees.setTextColor(android.graphics.Color.BLACK)
            panelTabEmployees.alpha = 1f
        } else {
            panelTabEmployees.setTextColor(android.graphics.Color.GRAY)
            panelTabEmployees.alpha = 0.5f
        }

        btnOpenPanel.setOnClickListener  { openPanel() }
        btnClosePanel.setOnClickListener { closePanel() }

        // Inventory tab — just close the panel since we're already on that page
        panelTabInventory.setOnClickListener {
            closePanel()
        }

        // Employees tab — navigate to EmployeesActivity
        panelTabEmployees.setOnClickListener {
            if (isAdminOfOrg) {
                closePanel()
                navigateToEmployees()
            } else {
                Toast.makeText(
                    this,
                    "Only the organization admin can manage employees",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun navigateToEmployees() {
        // Fetch current employees from Room to pass to EmployeesActivity
        lifecycleScope.launch {
            val localEmployees = withContext(Dispatchers.IO) {
                organizationRepository.getEmployeesForOrganization(orgId)
            }

            // Convert to EmployeeData
            val employeeList = ArrayList(localEmployees.map { entity ->
                EmployeeData(
                    id    = entity.id,
                    name  = entity.name,
                    email = entity.email
                )
            })

            val intent = Intent(this@OrganizationActivity, EmployeesActivity::class.java)
            intent.putExtra("org_id",    orgId)
            intent.putExtra("org_name",  orgName)
            intent.putExtra("employees", employeeList)
            startActivity(intent)
        }
    }

    private fun openPanel() {
        llSidePanel.visibility = View.VISIBLE
        val anim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
        llSidePanel.startAnimation(anim)
        isPanelOpen = true
    }

    private fun closePanel() {
        val anim = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_out_left)
        anim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(a: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
            override fun onAnimationEnd(a: android.view.animation.Animation?) {
                llSidePanel.visibility = View.GONE
            }
        })
        llSidePanel.startAnimation(anim)
        isPanelOpen = false
    }
}