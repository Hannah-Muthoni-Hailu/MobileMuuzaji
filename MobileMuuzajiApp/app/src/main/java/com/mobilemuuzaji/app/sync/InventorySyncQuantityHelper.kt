package com.mobilemuuzaji.app.sync

object InventorySyncQuantityHelper {

    fun quantityToCreateForNewItem(currentQuantity: Int, pendingSalesQuantity: Int): Int {
        return currentQuantity + pendingSalesQuantity
    }
}
