package com.mobilemuuzaji.app.database.entities

import androidx.room.Index
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory",
    foreignKeys = [ForeignKey(
        entity = OrganizationEntity::class,
        parentColumns = ["id"],
        childColumns = ["orgId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["orgId"])]
)
data class InventoryEntity(
    @PrimaryKey val id: Int,
    val itemName: String,
    val itemQuantity: Int,
    val unit: String,           // stored as string, mapped to Units enum in repository
    val costPerUnit: Int,
    val orgId: Int,
    val isSynced: Boolean = false
)