package com.mobilemuuzaji.app.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "organization_employees",
    primaryKeys = ["employeeId", "organizationId"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["employeeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = OrganizationEntity::class,
            parentColumns = ["id"],
            childColumns = ["organizationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OrganizationEmployeeCrossRef(
    val employeeId: Int,
    val organizationId: Int
)