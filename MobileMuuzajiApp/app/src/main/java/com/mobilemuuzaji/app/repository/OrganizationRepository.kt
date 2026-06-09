package com.mobilemuuzaji.app.repository

import com.mobilemuuzaji.app.database.dao.OrganizationDao
import com.mobilemuuzaji.app.database.entities.OrganizationEntity
import com.mobilemuuzaji.app.database.entities.OrganizationEmployeeCrossRef
import com.mobilemuuzaji.app.database.entities.UserEntity

class OrganizationRepository(private val organizationDao: OrganizationDao) {

    suspend fun getAllOrganizations(): List<OrganizationEntity> {
        return organizationDao.getAllOrganizations()
    }

    suspend fun getOrganizationById(id: Int): OrganizationEntity? {
        return organizationDao.getOrganizationById(id)
    }

    suspend fun getOrganizationsByAdmin(adminId: Int): List<OrganizationEntity> {
        return organizationDao.getOrganizationsByAdmin(adminId)
    }

    suspend fun saveOrganization(organization: OrganizationEntity) {
        organizationDao.insertOrganization(organization.copy(isSynced = false))
    }

    suspend fun updateOrganization(organization: OrganizationEntity) {
        organizationDao.updateOrganization(organization.copy(isSynced = false))
    }

    suspend fun deleteOrganization(organization: OrganizationEntity) {
        organizationDao.deleteOrganization(organization)
    }

    // Junction table operations
    suspend fun addEmployee(employeeId: Int, organizationId: Int) {
        organizationDao.addEmployee(
            OrganizationEmployeeCrossRef(
                employeeId = employeeId,
                organizationId = organizationId
            )
        )
    }

    suspend fun removeEmployee(employeeId: Int, organizationId: Int) {
        organizationDao.removeEmployee(
            OrganizationEmployeeCrossRef(
                employeeId = employeeId,
                organizationId = organizationId
            )
        )
    }

    suspend fun getEmployeesForOrganization(orgId: Int): List<UserEntity> {
        return organizationDao.getEmployeesForOrganization(orgId)
    }

    suspend fun getUnsyncedOrganizations(): List<OrganizationEntity> {
        return organizationDao.getUnsyncedOrganizations()
    }

    suspend fun markAsSynced(organization: OrganizationEntity) {
        organizationDao.updateOrganization(organization.copy(isSynced = true))
    }
}