package com.mobilemuuzaji.app.repository

import com.mobilemuuzaji.app.database.dao.UserDao
import com.mobilemuuzaji.app.database.entities.UserEntity

class UserRepository(private val userDao: UserDao) {

    suspend fun getAllUsers(): List<UserEntity> {
        return userDao.getAllUsers()
    }

    suspend fun getUserById(id: Int): UserEntity? {
        return userDao.getUserById(id)
    }

    suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }

    suspend fun saveUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(user: UserEntity) {
        userDao.deleteUser(user)
    }

    suspend fun getUnsyncedUsers(): List<UserEntity> {
        return userDao.getUnsyncedUsers()
    }

    suspend fun markAsSynced(user: UserEntity) {
        userDao.updateUser(user.copy(isSynced = true))
    }
}