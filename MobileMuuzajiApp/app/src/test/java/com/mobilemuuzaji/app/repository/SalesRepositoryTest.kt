package com.mobilemuuzaji.app.repository

import com.mobilemuuzaji.app.database.dao.SalesDao
import com.mobilemuuzaji.app.database.entities.SalesEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class SalesRepositoryTest {

    @Test
    fun saveSale_preservesExplicitSyncState() = runBlocking {
        val fakeDao = FakeSalesDao()
        val repository = SalesRepository(fakeDao)

        val sale = SalesEntity(
            id = 42,
            itemName = "Soap",
            itemQuantity = 3,
            buyingPrice = 100,
            sellingPrice = 150,
            grossIncome = 450,
            profit = 50,
            vatAmount = 0,
            orgId = 7,
            isSynced = true
        )

        repository.saveSale(sale)

        assertTrue(fakeDao.savedSales.single().isSynced)
    }

    private class FakeSalesDao : SalesDao {
        val savedSales = mutableListOf<SalesEntity>()

        override suspend fun getSalesForOrganization(orgId: Int): List<SalesEntity> = emptyList()

        override suspend fun getSalesForOrganizationSortedByDate(orgId: Int): List<SalesEntity> = emptyList()

        override suspend fun insertSale(sale: SalesEntity) {
            savedSales.clear()
            savedSales.add(sale)
        }

        override suspend fun deleteSale(sale: SalesEntity) {}

        override suspend fun getUnsyncedSales(): List<SalesEntity> = emptyList()

        override suspend fun getSaleById(id: Int): SalesEntity? = null
    }
}
