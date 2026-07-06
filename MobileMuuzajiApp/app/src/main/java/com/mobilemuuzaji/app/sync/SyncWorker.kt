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
                    val pendingSalesQuantity = salesRepo.getUnsyncedSales()
                        .filter { it.itemName == item.itemName && it.orgId == item.orgId }
                        .sumOf { it.itemQuantity }

                    val quantityToCreate = InventorySyncQuantityHelper.quantityToCreateForNewItem(
                        currentQuantity = item.itemQuantity,
                        pendingSalesQuantity = pendingSalesQuantity
                    )

                    val response = ApiClient.apiService.createInventoryItem(
                        NewInventoryRequest(
                            name          = item.itemName,
                            quantity      = quantityToCreate,
                            unit          = item.unit,
                            buying_price   = item.buyingPrice,
                            selling_price  = item.sellingPrice,
                            vat_percentage = item.vatPercentage,
                            org_id        = item.orgId
                        )
                    )

                    if (response.isSuccessful) {
                        val serverItem = response.body()!!

                        inventoryRepo.deleteInventoryItem(item)
                        inventoryRepo.saveInventoryItem(
                            item.copy(id = serverItem.id, isSynced = true)
                        )
                        Log.d("SyncWorker", "Created inventory item synced: ${item.itemName}")
                    } else {
                        val errorBody = response.errorBody()?.string().orEmpty()
                        val decision = SyncErrorClassifier.classify(response.code(), errorBody)
                        Log.e("SyncWorker", "Failed to sync new item: ${item.itemName} HTTP ${response.code()}: $errorBody")

                        if (decision.shouldDrop) {
                            inventoryRepo.deleteInventoryItem(item)
                            Log.w("SyncWorker", "Dropped permanently failed inventory item: ${item.itemName}")
                        } else {
                            allSucceeded = false
                        }
                    }

                } else {
                    // Positive id = edited offline — PUT to backend
                    val pendingSalesQuantity = salesRepo.getUnsyncedSales()
                        .filter { it.itemName == item.itemName && it.orgId == item.orgId }
                        .sumOf { it.itemQuantity }

                    val quantityToUpdate = InventorySyncQuantityHelper.quantityToCreateForNewItem(
                        currentQuantity = item.itemQuantity,
                        pendingSalesQuantity = pendingSalesQuantity
                    )

                    val response = ApiClient.apiService.updateInventoryItem(
                        itemId  = item.id,
                        request = UpdateInventoryRequest(
                            item_name     = item.itemName,
                            item_quantity = quantityToUpdate,
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
                        val errorBody = response.errorBody()?.string().orEmpty()
                        val decision = SyncErrorClassifier.classify(response.code(), errorBody)
                        Log.e("SyncWorker", "Failed to sync updated item: ${item.itemName} HTTP ${response.code()}: $errorBody")

                        if (decision.shouldDrop) {
                            inventoryRepo.deleteInventoryItem(item)
                            Log.w("SyncWorker", "Dropped permanently failed inventory item: ${item.itemName}")
                        } else {
                            allSucceeded = false
                        }
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
                    sales.forEach { sale ->
                        salesRepo.deleteSale(sale)
                    }
                    Log.w("SyncWorker", "Removed sales with missing inventory item from queue for: $itemName")
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

                    val syncedSale = sales.first().copy(
                        id           = serverSale.id,
                        itemQuantity = totalQuantitySold,
                        buyingPrice  = serverSale.buying_price,
                        sellingPrice = serverSale.selling_price,
                        grossIncome  = serverSale.gross_income,
                        profit       = serverSale.profit,
                        vatAmount    = serverSale.vat_amount,
                        isSynced     = true
                    )

                    salesRepo.saveSale(syncedSale)

                    val saved = salesRepo.getSaleById(serverSale.id)
                    Log.d(
                        "SyncWorker",
                        "Sale saved to Room: ${saved?.itemName} id=${saved?.id} synced=${saved?.isSynced}"
                    )
                    Log.d("SyncWorker", "Consolidated sale synced for: $itemName")

                } else {
                    val errorBody = response.errorBody()?.string().orEmpty()
                    val decision = SyncErrorClassifier.classify(response.code(), errorBody)
                    Log.e("SyncWorker", "Failed: $itemName HTTP ${response.code()}: $errorBody")

                    if (decision.shouldDrop) {
                        sales.forEach { sale ->
                            salesRepo.deleteSale(sale)
                        }
                        Log.w("SyncWorker", "Removed permanently failed sales from queue for: $itemName")
                    } else {
                        allSucceeded = false
                    }
                }

            } catch (e: Exception) {
                Log.e("SyncWorker", "Exception syncing sale $itemName: ${e.message}")
                allSucceeded = false
            }
        }

        return if (allSucceeded) Result.success() else Result.retry()
    }
}