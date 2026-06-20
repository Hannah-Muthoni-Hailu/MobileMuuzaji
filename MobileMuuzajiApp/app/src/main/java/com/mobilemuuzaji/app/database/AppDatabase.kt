package com.mobilemuuzaji.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mobilemuuzaji.app.database.dao.*
import com.mobilemuuzaji.app.database.entities.*

@Database(
    entities = [
        UserEntity::class,
        OrganizationEntity::class,
        OrganizationEmployeeCrossRef::class,
        InventoryEntity::class,
        SalesEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun organizationDao(): OrganizationDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun salesDao(): SalesDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mobilemuuzaji_db"
                ).fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
        }
    }
}