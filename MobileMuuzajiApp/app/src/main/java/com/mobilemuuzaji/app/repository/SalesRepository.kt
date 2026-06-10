package com.mobilemuuzaji.app.repository

import com.mobilemuuzaji.app.database.dao.SalesDao
import com.mobilemuuzaji.app.database.entities.SalesEntity

class SalesRepository(private val salesDao: SalesDao) {

    suspend fun getSalesForOrganization(orgId: Int): List<SalesEntity> {
        return salesDao.getSalesForOrganization(orgId)
    }

    suspend fun getSalesForOrganizationSortedByDate(orgId: Int): List<SalesEntity> {
        return salesDao.getSalesForOrganizationSortedByDate(orgId)
    }

    suspend fun saveSale(sale: SalesEntity) {
        salesDao.insertSale(sale.copy(isSynced = false))
    }

    suspend fun deleteSale(sale: SalesEntity) {
        salesDao.deleteSale(sale)
    }

    suspend fun getUnsyncedSales(): List<SalesEntity> {
        return salesDao.getUnsyncedSales()
    }

    suspend fun markAsSynced(sale: SalesEntity) {
        salesDao.insertSale(sale.copy(isSynced = true))
    }
}