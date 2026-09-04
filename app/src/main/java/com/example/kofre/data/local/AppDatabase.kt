package com.example.kofre.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.kofre.data.local.dao.CategoryDao
import com.example.kofre.data.local.dao.InvestmentContributionDao
import com.example.kofre.data.local.dao.InvestmentDao
import com.example.kofre.data.local.dao.MonthlyBudgetDao
import com.example.kofre.data.local.dao.TransactionDao
import com.example.kofre.data.local.entity.CategoryEntity
import com.example.kofre.data.local.entity.InvestmentContributionEntity
import com.example.kofre.data.local.entity.InvestmentEntity
import com.example.kofre.data.local.entity.MonthlyBudgetEntity
import com.example.kofre.data.local.entity.TransactionEntity

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        InvestmentEntity::class,
        InvestmentContributionEntity::class,
        MonthlyBudgetEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun investmentContributionDao(): InvestmentContributionDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao
}
