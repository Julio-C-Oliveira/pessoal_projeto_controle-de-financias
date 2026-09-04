package com.example.kofre.domain.repository

import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.model.Investment
import com.example.kofre.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

import com.example.kofre.domain.model.InvestmentContribution
import com.example.kofre.domain.model.MonthlyBudget

import com.example.kofre.data.local.backup.BackupPayloadDto

interface FinanceRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>
    suspend fun insertCategory(category: Category): Long
    suspend fun deleteCategory(category: Category)

    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    fun getTransactionsByGroupId(groupId: String): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun insertTransactions(transactions: List<Transaction>): List<Long>
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun deleteTransactionsByGroupId(groupId: String)

    fun getAllInvestments(): Flow<List<Investment>>
    fun getInvestmentById(id: Long): Flow<Investment?>
    suspend fun insertInvestment(investment: Investment): Long
    suspend fun updateInvestmentBalance(id: Long, newBalanceInCents: Long)

    fun getAllContributions(): Flow<List<InvestmentContribution>>
    fun getContributionsByInvestmentId(investmentId: Long): Flow<List<InvestmentContribution>>
    suspend fun insertContribution(contribution: InvestmentContribution): Long

    fun getBudgetsForMonth(year: Int, month: Int): Flow<List<MonthlyBudget>>
    fun getBudget(year: Int, month: Int, categoryId: Long): Flow<MonthlyBudget?>
    suspend fun insertBudget(budget: MonthlyBudget): Long
    suspend fun insertBudgets(budgets: List<MonthlyBudget>): List<Long>
    suspend fun deleteBudget(budgetId: Long)

    suspend fun exportBackup(): BackupPayloadDto
    suspend fun importBackup(payload: BackupPayloadDto)
}


