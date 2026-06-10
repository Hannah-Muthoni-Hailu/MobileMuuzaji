package com.mobilemuuzaji.app.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "organization",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["adminId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class OrganizationEntity(
    @PrimaryKey val id: Int,
    val orgName: String,
    val adminId: Int,
    val isSynced: Boolean = false
)