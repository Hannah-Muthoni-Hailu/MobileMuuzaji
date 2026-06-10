package com.mobilemuuzaji.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemuuzaji.app.database.entities.SalesEntity
import com.mobilemuuzaji.app.repository.SalesRepository
import kotlinx.coroutines.launch

class SalesViewModel(private val repository: SalesRepository) : ViewModel() {

    private val _sales = MutableLiveData<List<SalesEntity>>()
    val sales: LiveData<List<SalesEntity>> = _sales

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadSalesForOrganization(orgId: Int) {
        viewModelScope.launch {
            try {
                _sales.value = repository.getSalesForOrganization(orgId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadSalesSortedByDate(orgId: Int) {
        viewModelScope.launch {
            try {
                _sales.value = repository.getSalesForOrganizationSortedByDate(orgId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun saveSale(sale: SalesEntity) {
        viewModelScope.launch {
            try {
                repository.saveSale(sale)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteSale(sale: SalesEntity) {
        viewModelScope.launch {
            try {
                repository.deleteSale(sale)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}