package com.mobilemuuzaji.app.repository

import com.mobilemuuzaji.app.database.dao.InventoryDao
import com.mobilemuuzaji.app.database.entities.InventoryEntity

class InventoryRepository(private val inventoryDao: InventoryDao) {

    suspend fun getInventoryForOrganization(orgId: Int): List<InventoryEntity> {
        return inventoryDao.getInventoryForOrganization(orgId)
    }

    suspend fun getInventoryItemById(id: Int): InventoryEntity? {
        return inventoryDao.getInventoryItemById(id)
    }

    suspend fun saveInventoryItem(item: InventoryEntity) {
        inventoryDao.insertInventoryItem(item.copy(isSynced = false))
    }

    suspend fun updateInventoryItem(item: InventoryEntity) {
        inventoryDao.updateInventoryItem(item.copy(isSynced = false))
    }

    suspend fun deleteInventoryItem(item: InventoryEntity) {
        inventoryDao.deleteInventoryItem(item)
    }

    suspend fun getUnsyncedItems(): List<InventoryEntity> {
        return inventoryDao.getUnsyncedItems()
    }

    suspend fun markAsSynced(item: InventoryEntity) {
        inventoryDao.updateInventoryItem(item.copy(isSynced = true))
    }
}