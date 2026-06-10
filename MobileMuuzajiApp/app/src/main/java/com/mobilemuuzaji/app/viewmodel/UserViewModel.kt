package com.mobilemuuzaji.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilemuuzaji.app.database.entities.UserEntity
import com.mobilemuuzaji.app.repository.UserRepository
import kotlinx.coroutines.launch

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _users = MutableLiveData<List<UserEntity>>()
    val users: LiveData<List<UserEntity>> = _users

    private val _currentUser = MutableLiveData<UserEntity?>()
    val currentUser: LiveData<UserEntity?> = _currentUser

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadAllUsers() {
        viewModelScope.launch {
            try {
                _users.value = repository.getAllUsers()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadUserByEmail(email: String) {
        viewModelScope.launch {
            try {
                _currentUser.value = repository.getUserByEmail(email)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun saveUser(user: UserEntity) {
        viewModelScope.launch {
            try {
                repository.saveUser(user)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateUser(user: UserEntity) {
        viewModelScope.launch {
            try {
                repository.updateUser(user)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            try {
                repository.deleteUser(user)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}