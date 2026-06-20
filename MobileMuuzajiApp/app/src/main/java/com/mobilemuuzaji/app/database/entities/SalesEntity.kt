package com.mobilemuuzaji.app.database.entities

import androidx.room.Index
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    foreignKeys = [ForeignKey(
        entity = OrganizationEntity::class,
        parentColumns = ["id"],
        childColumns = ["orgId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["orgId"])]
)
data class SalesEntity(
    @PrimaryKey val id: Int,
    val itemName:       String,
    val itemQuantity:   Int,
    val buyingPrice:    Int,
    val sellingPrice:   Int,
    val grossIncome:    Int,
    val profit:         Int,
    val vatAmount:      Int?   = null,
    val date:           Long   = System.currentTimeMillis(),
    val orgId:          Int,
    val isSynced:       Boolean = false,
    val salePrice:      Int?   = null,
    val updatePrice:    Boolean = false
)