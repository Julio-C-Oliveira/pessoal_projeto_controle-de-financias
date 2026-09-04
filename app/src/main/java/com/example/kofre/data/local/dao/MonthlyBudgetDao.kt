package com.example.kofre.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.kofre.data.local.entity.MonthlyBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyBudgetDao {

    @Query("SELECT * FROM monthly_budgets WHERE year = :year AND month = :month")
    fun getBudgetsForMonth(year: Int, month: Int): Flow<List<MonthlyBudgetEntity>>

    @Query("SELECT * FROM monthly_budgets WHERE year = :year AND month = :month AND categoryId = :categoryId LIMIT 1")
    fun getBudget(year: Int, month: Int, categoryId: Long): Flow<MonthlyBudgetEntity?>

    @Query("SELECT * FROM monthly_budgets")
    fun getAllBudgets(): Flow<List<MonthlyBudgetEntity>>

    @Upsert
    suspend fun upsertBudget(budget: MonthlyBudgetEntity): Long

    @Upsert
    suspend fun upsertBudgets(budgets: List<MonthlyBudgetEntity>): List<Long>

    @Query("DELETE FROM monthly_budgets WHERE id = :id")
    suspend fun deleteBudgetById(id: Long)

    @Query("DELETE FROM monthly_budgets")
    suspend fun deleteAllBudgets()
}
