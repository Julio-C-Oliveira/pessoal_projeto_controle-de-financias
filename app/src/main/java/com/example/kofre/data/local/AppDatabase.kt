package com.example.kofre.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.kofre.data.local.dao.CategoryDao
import com.example.kofre.data.local.dao.InvestmentDao
import com.example.kofre.data.local.dao.TransactionDao
import com.example.kofre.data.local.entity.CategoryEntity
import com.example.kofre.data.local.entity.InvestmentEntity
import com.example.kofre.data.local.entity.TransactionEntity

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        InvestmentEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun investmentDao(): InvestmentDao
}
