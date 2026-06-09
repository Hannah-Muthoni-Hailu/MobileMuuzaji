package com.mobilemuuzaji.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.mobilemuuzaji.app.database.AppDatabase
import com.mobilemuuzaji.app.database.entities.*
import com.mobilemuuzaji.app.repository.*
import kotlinx.coroutines.launch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var tvResults: TextView
    private lateinit var btnRunTests: Button
    private val results = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvResults   = findViewById(R.id.tvResults)
        btnRunTests = findViewById(R.id.btnRunTests)

        btnRunTests.setOnClickListener {
            results.clear()
            tvResults.text = "Running tests...\n"
            runAllTests()
        }
    }

    private fun runAllTests() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val db = AppDatabase.getInstance(applicationContext)

                    val userRepository         = UserRepository(db.userDao())
                    val organizationRepository = OrganizationRepository(db.organizationDao())
                    val inventoryRepository    = InventoryRepository(db.inventoryDao())
                    val salesRepository        = SalesRepository(db.salesDao())

                    clearDatabase(db)
                    log("Clearing existing data...")

                    testUsers(userRepository)
                    testOrganizations(userRepository, organizationRepository)
                    testEmployees(organizationRepository)
                    testInventory(inventoryRepository)
                    testSales(salesRepository)
                }

                log("\n✓ All tests passed!")

            } catch (e: Exception) {
                log("\n✗ Test failed: ${e.message}")
            }

            tvResults.text = results.toString()
        }
    }

    private suspend fun testUsers(userRepository: UserRepository) {
        log("\n── User tests ──")

        // Create
        val user = UserEntity(id = 1, name = "Hannah", email = "hannah@test.com", password = "hashed_password")
        userRepository.saveUser(user)
        log("✓ User created")

        // Read
        val fetchedUser = userRepository.getUserById(1)
        assert(fetchedUser != null) { "User should not be null" }
        assert(fetchedUser?.name == "Hannah") { "Name should be Hannah" }
        log("✓ User fetched: ${fetchedUser?.name}")

        // Read by email
        val fetchedByEmail = userRepository.getUserByEmail("hannah@test.com")
        assert(fetchedByEmail != null) { "User fetched by email should not be null" }
        log("✓ User fetched by email: ${fetchedByEmail?.email}")

        // Update
        userRepository.updateUser(user.copy(name = "Hannah Updated"))
        val updatedUser = userRepository.getUserById(1)
        assert(updatedUser?.name == "Hannah Updated") { "Name should be updated" }
        log("✓ User updated: ${updatedUser?.name}")

        // Unsynced check
        val unsyncedUsers = userRepository.getUnsyncedUsers()
        assert(unsyncedUsers.isNotEmpty()) { "Should have unsynced users" }
        log("✓ Unsynced users found: ${unsyncedUsers.size}")

        // Mark as synced
        userRepository.markAsSynced(user)
        val stillUnsynced = userRepository.getUnsyncedUsers()
        assert(stillUnsynced.isEmpty()) { "Should have no unsynced users" }
        log("✓ User marked as synced")
    }

    private suspend fun testOrganizations(
        userRepository: UserRepository,
        organizationRepository: OrganizationRepository
    ) {
        log("\n── Organization tests ──")

        // Create a second user to act as admin
        val admin = UserEntity(id = 2, name = "Admin User", email = "admin@test.com", password = "hashed_password")
        userRepository.saveUser(admin)

        // Create organization
        val org = OrganizationEntity(id = 1, orgName = "Test Org", adminId = 2)
        organizationRepository.saveOrganization(org)
        log("✓ Organization created")

        // Read
        val fetchedOrg = organizationRepository.getOrganizationById(1)
        assert(fetchedOrg != null) { "Organization should not be null" }
        assert(fetchedOrg?.orgName == "Test Org") { "Org name should be Test Org" }
        log("✓ Organization fetched: ${fetchedOrg?.orgName}")

        // Read by admin
        val orgsByAdmin = organizationRepository.getOrganizationsByAdmin(2)
        assert(orgsByAdmin.isNotEmpty()) { "Should find organizations by admin" }
        log("✓ Organizations by admin found: ${orgsByAdmin.size}")

        // Update
        organizationRepository.updateOrganization(org.copy(orgName = "Updated Org"))
        val updatedOrg = organizationRepository.getOrganizationById(1)
        assert(updatedOrg?.orgName == "Updated Org") { "Org name should be updated" }
        log("✓ Organization updated: ${updatedOrg?.orgName}")
    }

    private suspend fun testEmployees(organizationRepository: OrganizationRepository) {
        log("\n── Employee (junction table) tests ──")

        // Add employee to organization (user id=1, org id=1)
        organizationRepository.addEmployee(employeeId = 1, organizationId = 1)
        log("✓ Employee added to organization")

        // Fetch employees
        val employees = organizationRepository.getEmployeesForOrganization(1)
        assert(employees.isNotEmpty()) { "Should have employees" }
        log("✓ Employees fetched: ${employees.size}")

        // Remove employee
        organizationRepository.removeEmployee(employeeId = 1, organizationId = 1)
        val employeesAfterRemoval = organizationRepository.getEmployeesForOrganization(1)
        assert(employeesAfterRemoval.isEmpty()) { "Should have no employees after removal" }
        log("✓ Employee removed successfully")
    }

    private suspend fun testInventory(inventoryRepository: InventoryRepository) {
        log("\n── Inventory tests ──")

        // Create
        val item = InventoryEntity(
            id           = 1,
            itemName     = "Sugar",
            itemQuantity = 100,
            unit         = "kilogram",
            costPerUnit  = 50,
            orgId        = 1
        )
        inventoryRepository.saveInventoryItem(item)
        log("✓ Inventory item created")

        // Read
        val fetchedItem = inventoryRepository.getInventoryItemById(1)
        assert(fetchedItem != null) { "Inventory item should not be null" }
        assert(fetchedItem?.itemName == "Sugar") { "Item name should be Sugar" }
        log("✓ Inventory item fetched: ${fetchedItem?.itemName}")

        // Read by organization
        val orgItems = inventoryRepository.getInventoryForOrganization(1)
        assert(orgItems.isNotEmpty()) { "Should have inventory items for organization" }
        log("✓ Inventory for organization fetched: ${orgItems.size} items")

        // Update
        inventoryRepository.updateInventoryItem(item.copy(itemQuantity = 200))
        val updatedItem = inventoryRepository.getInventoryItemById(1)
        assert(updatedItem?.itemQuantity == 200) { "Quantity should be updated" }
        log("✓ Inventory item updated: quantity = ${updatedItem?.itemQuantity}")

        // Unsynced check
        val unsyncedItems = inventoryRepository.getUnsyncedItems()
        assert(unsyncedItems.isNotEmpty()) { "Should have unsynced items" }
        log("✓ Unsynced items found: ${unsyncedItems.size}")

        // Mark as synced
        inventoryRepository.markAsSynced(item)
        log("✓ Inventory item marked as synced")
    }

    private suspend fun testSales(salesRepository: SalesRepository) {
        log("\n── Sales tests ──")

        // Create
        val sale = SalesEntity(
            id           = 1,
            itemName     = "Sugar",
            itemQuantity = 10,
            earnings     = 500,
            orgId        = 1
        )
        salesRepository.saveSale(sale)
        log("✓ Sale created")

        // Read
        val orgSales = salesRepository.getSalesForOrganization(1)
        assert(orgSales.isNotEmpty()) { "Should have sales for organization" }
        log("✓ Sales fetched: ${orgSales.size}")

        // Read sorted by date
        val sortedSales = salesRepository.getSalesForOrganizationSortedByDate(1)
        assert(sortedSales.isNotEmpty()) { "Should have sorted sales" }
        log("✓ Sales sorted by date fetched: ${sortedSales.size}")

        // Unsynced check
        val unsyncedSales = salesRepository.getUnsyncedSales()
        assert(unsyncedSales.isNotEmpty()) { "Should have unsynced sales" }
        log("✓ Unsynced sales found: ${unsyncedSales.size}")

        // Mark as synced
        salesRepository.markAsSynced(sale)
        log("✓ Sale marked as synced")

        // Delete
        salesRepository.deleteSale(sale)
        val salesAfterDelete = salesRepository.getSalesForOrganization(1)
        assert(salesAfterDelete.isEmpty()) { "Should have no sales after delete" }
        log("✓ Sale deleted successfully")
    }

    private suspend fun clearDatabase(db: AppDatabase) {
        db.clearAllTables()
    }

    private fun log(message: String) {
        results.appendLine(message)
    }
}