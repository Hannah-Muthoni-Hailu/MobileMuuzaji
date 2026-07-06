package com.mobilemuuzaji.app.database.dao

import androidx.room.*
import com.mobilemuuzaji.app.database.entities.SalesEntity

@Dao
interface SalesDao {
    @Query("SELECT * FROM sales WHERE orgId = :orgId")
    suspend fun getSalesForOrganization(orgId: Int): List<SalesEntity>

    @Query("SELECT * FROM sales WHERE orgId = :orgId ORDER BY date DESC")
    suspend fun getSalesForOrganizationSortedByDate(orgId: Int): List<SalesEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SalesEntity)

    @Delete
    suspend fun deleteSale(sale: SalesEntity)

    @Query("SELECT * FROM sales WHERE isSynced = 0")
    suspend fun getUnsyncedSales(): List<SalesEntity>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Int): SalesEntity?
}