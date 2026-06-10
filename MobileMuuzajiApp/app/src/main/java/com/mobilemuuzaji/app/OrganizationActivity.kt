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
    private var isSearchVisible = false

    private lateinit var inventoryRepository: InventoryRepository
    private lateinit var salesRepository:     SalesRepository
    private lateinit var organizationRepository: OrganizationRepository
    private val db by lazy { AppDatabase.getInstance(applicationContext) }

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
            // TODO: implement new inventory
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
                }
            }
        })

        // Load from local Room database first
        loadFromLocal()

        // If online, fetch fresh data from API and update Room
        if (NetworkUtils.isOnline(this)) {
            fetchFromApi()
        }
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

        if (toInventory) {
            tabInventory.setTextColor(Color.parseColor("#1976D2"))
            tabInventory.setTypeface(null, android.graphics.Typeface.BOLD)
            tabSales.setTextColor(Color.parseColor("#888888"))
            tabSales.setTypeface(null, android.graphics.Typeface.NORMAL)
        } else {
            tabSales.setTextColor(Color.parseColor("#1976D2"))
            tabSales.setTypeface(null, android.graphics.Typeface.BOLD)
            tabInventory.setTextColor(Color.parseColor("#888888"))
            tabInventory.setTypeface(null, android.graphics.Typeface.NORMAL)
        }

        refreshList()   // ← call refreshList instead of updateList
    }

    private fun refreshList() {
        // Clear search when switching tabs
        etSearch.text.clear()
        
        if (showingInventory) {
            lvItems.adapter         = InventoryAdapter(this, inventoryItems)
            tvEmptyState.text       = "No inventory items"
            tvEmptyState.visibility = if (inventoryItems.isEmpty()) View.VISIBLE else View.GONE
        } else {
            lvItems.adapter         = SalesAdapter(this, salesItems)
            tvEmptyState.text       = "No sales recorded"
            tvEmptyState.visibility = if (salesItems.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun updateList() {
        refreshList()                  // update the list
        switchTab(showingInventory)    // update the tab styles
    }
}