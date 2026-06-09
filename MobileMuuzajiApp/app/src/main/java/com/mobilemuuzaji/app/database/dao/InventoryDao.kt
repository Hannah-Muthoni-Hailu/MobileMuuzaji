package com.mobilemuuzaji.app.database.dao

import androidx.room.*
import com.mobilemuuzaji.app.database.entities.InventoryEntity

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory WHERE orgId = :orgId")
    suspend fun getInventoryForOrganization(orgId: Int): List<InventoryEntity>

    @Query("SELECT * FROM inventory WHERE id = :id")
    suspend fun getInventoryItemById(id: Int): InventoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryEntity)

    @Update
    suspend fun updateInventoryItem(item: InventoryEntity)

    @Delete
    suspend fun deleteInventoryItem(item: InventoryEntity)

    @Query("SELECT * FROM inventory WHERE isSynced = 0")
    suspend fun getUnsyncedItems(): List<InventoryEntity>
}