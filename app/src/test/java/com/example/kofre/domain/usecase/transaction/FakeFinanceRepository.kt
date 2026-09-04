package com.example.kofre.domain.usecase.transaction

import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.model.Investment
import com.example.kofre.domain.model.Transaction
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeFinanceRepository : FinanceRepository {

    private val categories = mutableListOf<Category>()
    private val transactions = mutableListOf<Transaction>()
    private val investments = mutableListOf<Investment>()

    private var nextCategoryId = 1L
    private var nextTransactionId = 1L
    private var nextInvestmentId = 1L

    override fun getAllCategories(): Flow<List<Category>> {
        val parents = categories.filter { it.parentId == null }
        val tree = parents.map { parent ->
            val children = categories.filter { it.parentId == parent.id }
            parent.copy(subcategories = children)
        }
        return flowOf(tree)
    }

    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> {
        val parents = categories.filter { it.parentId == null && it.type == type }
        val tree = parents.map { parent ->
            val children = categories.filter { it.parentId == parent.id && it.type == type }
            parent.copy(subcategories = children)
        }
        return flowOf(tree)
    }

    override suspend fun insertCategory(category: Category): Long {
        val id = if (category.id == 0L) nextCategoryId++ else category.id
        val newCat = category.copy(id = id)
        categories.add(newCat)
        return id
    }

    override suspend fun deleteCategory(category: Category) {
        categories.removeAll { it.id == category.id }
    }

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return flowOf(transactions.toList())
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        val filtered = transactions.filter { it.timestamp in startDate..endDate }
        return flowOf(filtered)
    }

    override fun getTransactionsByGroupId(groupId: String): Flow<List<Transaction>> {
        val filtered = transactions.filter { it.installmentGroupId == groupId }
        return flowOf(filtered)
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        val id = if (transaction.id == 0L) nextTransactionId++ else transaction.id
        val newTx = transaction.copy(id = id)
        transactions.add(newTx)
        return id
    }

    override suspend fun insertTransactions(transactionsList: List<Transaction>): List<Long> {
        return transactionsList.map { insertTransaction(it) }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactions.removeAll { it.id == transaction.id }
    }

    override suspend fun deleteTransactionsByGroupId(groupId: String) {
        transactions.removeAll { it.installmentGroupId == groupId }
    }

    override fun getAllInvestments(): Flow<List<Investment>> {
        return flowOf(investments.toList())
    }

    override suspend fun insertInvestment(investment: Investment): Long {
        val id = if (investment.id == 0L) nextInvestmentId++ else investment.id
        val newInv = investment.copy(id = id)
        investments.add(newInv)
        return id
    }

    override suspend fun updateInvestmentBalance(id: Long, newBalanceInCents: Long) {
        val index = investments.indexOfFirst { it.id == id }
        if (index != -1) {
            investments[index] = investments[index].copy(currentBalanceInCents = newBalanceInCents)
        }
    }
}
