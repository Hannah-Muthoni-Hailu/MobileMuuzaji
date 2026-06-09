// OrganizationViewModelFactory.kt
package com.mobilemuuzaji.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mobilemuuzaji.app.repository.OrganizationRepository

class OrganizationViewModelFactory(private val repository: OrganizationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OrganizationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OrganizationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}