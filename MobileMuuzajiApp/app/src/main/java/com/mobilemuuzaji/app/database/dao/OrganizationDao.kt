package com.mobilemuuzaji.app.database.dao

import androidx.room.*
import com.mobilemuuzaji.app.database.entities.OrganizationEntity
import com.mobilemuuzaji.app.database.entities.OrganizationEmployeeCrossRef

@Dao
interface OrganizationDao {
    @Query("SELECT * FROM organization")
    suspend fun getAllOrganizations(): List<OrganizationEntity>

    @Query("SELECT * FROM organization WHERE id = :id")
    suspend fun getOrganizationById(id: Int): OrganizationEntity?

    @Query("SELECT * FROM organization WHERE adminId = :adminId")
    suspend fun getOrganizationsByAdmin(adminId: Int): List<OrganizationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrganization(organization: OrganizationEntity)

    @Update
    suspend fun updateOrganization(organization: OrganizationEntity)

    @Delete
    suspend fun deleteOrganization(organization: OrganizationEntity)

    // Junction table operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addEmployee(crossRef: OrganizationEmployeeCrossRef)

    @Delete
    suspend fun removeEmployee(crossRef: OrganizationEmployeeCrossRef)

    @Query("SELECT * FROM users INNER JOIN organization_employees ON users.id = organization_employees.employeeId WHERE organization_employees.organizationId = :orgId")
    suspend fun getEmployeesForOrganization(orgId: Int): List<com.mobilemuuzaji.app.database.entities.UserEntity>

    @Query("SELECT * FROM organization WHERE isSynced = 0")
    suspend fun getUnsyncedOrganizations(): List<OrganizationEntity>
}