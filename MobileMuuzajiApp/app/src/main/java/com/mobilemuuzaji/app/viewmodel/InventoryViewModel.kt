package com.mobilemuuzaji.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemuuzaji.app.database.entities.InventoryEntity
import com.mobilemuuzaji.app.repository.InventoryRepository
import kotlinx.coroutines.launch

class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {

    private val _inventoryItems = MutableLiveData<List<InventoryEntity>>()
    val inventoryItems: LiveData<List<InventoryEntity>> = _inventoryItems

    private val _currentItem = MutableLiveData<InventoryEntity?>()
    val currentItem: LiveData<InventoryEntity?> = _currentItem

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadInventoryForOrganization(orgId: Int) {
        viewModelScope.launch {
            try {
                _inventoryItems.value = repository.getInventoryForOrganization(orgId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadInventoryItemById(id: Int) {
        viewModelScope.launch {
            try {
                _currentItem.value = repository.getInventoryItemById(id)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun saveInventoryItem(item: InventoryEntity) {
        viewModelScope.launch {
            try {
                repository.saveInventoryItem(item)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateInventoryItem(item: InventoryEntity) {
         viewModelScope.launch {
            try {
                repository.updateInventoryItem(item)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteInventoryItem(item: InventoryEntity) {
        viewModelScope.launch {
            try {
                repository.deleteInventoryItem(item)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}