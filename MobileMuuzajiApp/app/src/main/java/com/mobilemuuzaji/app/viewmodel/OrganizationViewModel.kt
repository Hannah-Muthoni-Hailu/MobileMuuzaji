package com.mobilemuuzaji.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemuuzaji.app.database.entities.OrganizationEntity
import com.mobilemuuzaji.app.database.entities.UserEntity
import com.mobilemuuzaji.app.repository.OrganizationRepository
import kotlinx.coroutines.launch

class OrganizationViewModel(private val repository: OrganizationRepository) : ViewModel() {

    private val _organizations = MutableLiveData<List<OrganizationEntity>>()
    val organizations: LiveData<List<OrganizationEntity>> = _organizations

    private val _currentOrganization = MutableLiveData<OrganizationEntity?>()
    val currentOrganization: LiveData<OrganizationEntity?> = _currentOrganization

    private val _employees = MutableLiveData<List<UserEntity>>()
    val employees: LiveData<List<UserEntity>> = _employees

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadOrganizationsByAdmin(adminId: Int) {
        viewModelScope.launch {
            try {
                _organizations.value = repository.getOrganizationsByAdmin(adminId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadOrganizationById(id: Int) {
        viewModelScope.launch {
            try {
                _currentOrganization.value = repository.getOrganizationById(id)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun saveOrganization(organization: OrganizationEntity) {
        viewModelScope.launch {
            try {
                repository.saveOrganization(organization)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateOrganization(organization: OrganizationEntity) {
        viewModelScope.launch {
            try {
                repository.updateOrganization(organization)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteOrganization(organization: OrganizationEntity) {
        viewModelScope.launch {
            try {
                repository.deleteOrganization(organization)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun addEmployee(employeeId: Int, organizationId: Int) {
        viewModelScope.launch {
            try {
                repository.addEmployee(employeeId, organizationId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun removeEmployee(employeeId: Int, organizationId: Int) {
        viewModelScope.launch {
            try {
                repository.removeEmployee(employeeId, organizationId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadEmployeesForOrganization(orgId: Int) {
        viewModelScope.launch {
            try {
                _employees.value = repository.getEmployeesForOrganization(orgId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}