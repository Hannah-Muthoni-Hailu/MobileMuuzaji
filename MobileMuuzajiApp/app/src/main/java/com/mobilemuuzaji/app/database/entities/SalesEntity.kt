package com.mobilemuuzaji.app.database.entities

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
    )]
)
data class SalesEntity(
    @PrimaryKey val id: Int,
    val itemName: String,
    val itemQuantity: Int,
    val earnings: Int,
    val date: Long = System.currentTimeMillis(),   // stored as timestamp in SQLite
    val orgId: Int,
    val isSynced: Boolean = false
)