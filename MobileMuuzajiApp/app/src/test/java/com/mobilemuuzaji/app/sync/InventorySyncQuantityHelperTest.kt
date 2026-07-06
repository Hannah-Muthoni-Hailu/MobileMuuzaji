package com.mobilemuuzaji.app.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class InventorySyncQuantityHelperTest {

    @Test
    fun addsPendingOfflineSalesWhenCreatingANewInventoryItem() {
        val quantityToCreate = InventorySyncQuantityHelper.quantityToCreateForNewItem(
            currentQuantity = 10,
            pendingSalesQuantity = 10
        )

        assertEquals(20, quantityToCreate)
    }
}
