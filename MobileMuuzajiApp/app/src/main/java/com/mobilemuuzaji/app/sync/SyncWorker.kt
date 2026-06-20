package com.mobilemuuzaji.app.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mobilemuuzaji.app.database.AppDatabase
import com.mobilemuuzaji.app.network.ApiClient
import com.mobilemuuzaji.app.network.models.NewInventoryRequest
import com.mobilemuuzaji.app.network.models.SaleRequest
import com.mobilemuuzaji.app.network.models.UpdateInventoryRequest
import com.mobilemuuzaji.app.repository.InventoryRepository
import com.mobilemuuzaji.app.repository.SalesRepository

class SyncWorker(
    context: Context,
    params:  WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db                = AppDatabase.getInstance(applicationContext)
        val inventoryRepo     = InventoryRepository(db.inventoryDao())
        val salesRepo         = SalesRepository(db.salesDao())

        var allSucceeded = true

        // ── Sync unsynced inventory items ──────────────────────────────
        val unsyncedInventory = inventoryRepo.getUnsyncedItems()
        Log.d("SyncWorker", "Unsynced inventory items: ${unsyncedInventory.size}")

        unsyncedInventory.forEach { item ->
            try {
                if (item.id < 0) {
                    // Negative id = created offline — POST to backend
                    val response = ApiClient.apiService.createInventoryItem(
                        NewInventoryRequest(
                            name          = item.itemName,
                            quantity      = item.itemQuantity,
                            unit          = item.unit,
                            buying_price   = item.buyingPrice,
                            selling_price  = item.sellingPrice,
                            vat_percentage = item.vatPercentage,
                            org_id        = item.orgId
                        )
                    )

                    if (response.isSuccessful) {
                        val serverItem = response.body()!!

                        // Delete the temp item and save with the real server id
                        inventoryRepo.deleteInventoryItem(item)
                        inventoryRepo.saveInventoryItem(
                            item.copy(id = serverItem.id, isSynced = true)
                        )
                        Log.d("SyncWorker", "Created inventory item synced: ${item.itemName}")
                    } else {
                        Log.e("SyncWorker", "Failed to sync new item: ${item.itemName}")
                        allSucceeded = false
                    }

                } else {
                    // Positive id = edited offline — PUT to backend
                    val response = ApiClient.apiService.updateInventoryItem(
                        itemId  = item.id,
                        request = UpdateInventoryRequest(
                            item_name     = item.itemName,
                            item_quantity = item.itemQuantity,
                            unit          = item.unit,
                            buying_price   = item.buyingPrice,
                            selling_price  = item.sellingPrice,
                            vat_percentage = item.vatPercentage,
                            org_id        = item.orgId
                        )
                    )

                    if (response.isSuccessful) {
                        inventoryRepo.markAsSynced(item)
                        Log.d("SyncWorker", "Updated inventory item synced: ${item.itemName}")
                    } else {
                        Log.e("SyncWorker", "Failed to sync updated item: ${item.itemName}")
                        allSucceeded = false
                    }
                }

            } catch (e: Exception) {
                Log.e("SyncWorker", "Exception syncing inventory item: ${e.message}")
                allSucceeded = false
            }
        }

        // ── Sync unsynced sales ────────────────────────────────────────
        val unsyncedSales = salesRepo.getUnsyncedSales()
        Log.d("SyncWorker", "Unsynced sales: ${unsyncedSales.size}")

        val groupedSales = unsyncedSales.groupBy { it.itemName }

        groupedSales.forEach { (itemName, sales) ->
            try {
                val inventoryItem = db.inventoryDao()
                    .getInventoryForOrganization(sales.first().orgId)
                    .firstOrNull { it.itemName == itemName && it.id > 0 }

                if (inventoryItem == null) {
                    val allItems = db.inventoryDao()
                        .getInventoryForOrganization(sales.first().orgId)
                    Log.e("SyncWorker", "No inventory item found for: $itemName")
                    Log.e("SyncWorker", "Available: ${allItems.map { "${it.itemName} id=${it.id}" }}")
                    allSucceeded = false
                    return@forEach
                }

                // Sum all offline sales for this item into one request
                val totalQuantitySold = sales.sumOf { it.itemQuantity }
                Log.d("SyncWorker", "Syncing $totalQuantitySold total of $itemName across ${sales.size} sales")

                val response = ApiClient.apiService.makeSale(
                    SaleRequest(
                        item_id       = inventoryItem.id,
                        quantity_sold = totalQuantitySold,
                        sale_price    = sales.first().salePrice,
                        update_price  = sales.first().updatePrice
                    )
                )

                if (response.isSuccessful) {
                    val serverSale = response.body()!!

                    // Delete all the individual temp sales
                    sales.forEach { sale ->
                        salesRepo.deleteSale(sale)
                    }

                    // Save one consolidated synced sale
                    salesRepo.saveSale(
                        sales.first().copy(
                            id           = serverSale.id,
                            itemQuantity = totalQuantitySold,
                            buyingPrice  = serverSale.buying_price,    // ← new
                            sellingPrice = serverSale.selling_price,   // ← new
                            grossIncome  = serverSale.gross_income,    // ← new
                            profit       = serverSale.profit,          // ← new
                            vatAmount    = serverSale.vat_amount,      // ← new
                            isSynced     = true
                        )
                    )
                    Log.d("SyncWorker", "Consolidated sale synced for: $itemName")

                } else {
                    Log.e("SyncWorker", "Failed: $itemName HTTP ${response.code()}: ${response.errorBody()?.string()}")
                    allSucceeded = false
                }

            } catch (e: Exception) {
                Log.e("SyncWorker", "Exception syncing sale $itemName: ${e.message}")
                allSucceeded = false
            }
        }

        return if (allSucceeded) Result.success() else Result.retry()
    }
}