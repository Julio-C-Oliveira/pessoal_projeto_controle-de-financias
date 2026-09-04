package com.example.kofre.data.repository

import com.example.kofre.data.local.dao.CategoryDao
import com.example.kofre.data.local.dao.InvestmentDao
import com.example.kofre.data.local.dao.TransactionDao
import com.example.kofre.data.local.entity.CategoryEntity
import com.example.kofre.data.local.entity.InvestmentEntity
import com.example.kofre.data.local.entity.TransactionEntity
import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.model.Investment
import com.example.kofre.domain.model.Transaction
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FinanceRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val investmentDao: InvestmentDao
) : FinanceRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            mapCategoryTree(entities)
        }
    }

    override fun getCategoriesByType(type: CategoryType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type.name).map { entities ->
            mapCategoryTree(entities)
        }
    }

    override suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category.toEntity())
    }

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return combine(
            transactionDao.getAllTransactions(),
            categoryDao.getAllCategories()
        ) { transactions, categories ->
            val categoryMap = categories.associateBy { it.id }
            transactions.map { it.toDomain(categoryMap[it.categoryId]?.toDomainFlat()) }
        }
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return combine(
            transactionDao.getTransactionsByDateRange(startDate, endDate),
            categoryDao.getAllCategories()
        ) { transactions, categories ->
            val categoryMap = categories.associateBy { it.id }
            transactions.map { it.toDomain(categoryMap[it.categoryId]?.toDomainFlat()) }
        }
    }

    override fun getTransactionsByGroupId(groupId: String): Flow<List<Transaction>> {
        return combine(
            transactionDao.getTransactionsByGroupId(groupId),
            categoryDao.getAllCategories()
        ) { transactions, categories ->
            val categoryMap = categories.associateBy { it.id }
            transactions.map { it.toDomain(categoryMap[it.categoryId]?.toDomainFlat()) }
        }
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction.toEntity())
    }

    override suspend fun insertTransactions(transactions: List<Transaction>): List<Long> {
        return transactionDao.insertTransactions(transactions.map { it.toEntity() })
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransactionsByGroupId(groupId: String) {
        transactionDao.deleteTransactionsByGroupId(groupId)
    }

    override fun getAllInvestments(): Flow<List<Investment>> {
        return investmentDao.getAllInvestments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertInvestment(investment: Investment): Long {
        return investmentDao.insertInvestment(investment.toEntity())
    }

    override suspend fun updateInvestmentBalance(id: Long, newBalanceInCents: Long) {
        investmentDao.updateBalance(id, newBalanceInCents)
    }

    // --- Helpers ---

    private fun mapCategoryTree(entities: List<CategoryEntity>): List<Category> {
        val parents = entities.filter { it.parentId == null }
        return parents.map { parent ->
            val children = entities.filter { it.parentId == parent.id }
                .map { it.toDomainFlat() }
            parent.toDomainFlat().copy(subcategories = children)
        }
    }

    private fun CategoryEntity.toDomainFlat(): Category {
        return Category(
            id = id,
            name = name,
            type = CategoryType.valueOf(type),
            parentId = parentId
        )
    }

    private fun Category.toEntity(): CategoryEntity {
        return CategoryEntity(
            id = id,
            name = name,
            type = type.name,
            parentId = parentId
        )
    }

    private fun TransactionEntity.toDomain(category: Category?): Transaction {
        return Transaction(
            id = id,
            amountInCents = amountInCents,
            timestamp = timestamp,
            categoryId = categoryId,
            category = category,
            type = TransactionType.valueOf(type),
            paymentMethod = PaymentMethod.valueOf(paymentMethod),
            isEssential = isEssential,
            installmentGroupId = installmentGroupId,
            installmentsCount = installmentsCount,
            currentInstallment = currentInstallment,
            notes = notes
        )
    }

    private fun Transaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            amountInCents = amountInCents,
            timestamp = timestamp,
            categoryId = categoryId,
            type = type.name,
            paymentMethod = paymentMethod.name,
            isEssential = isEssential,
            installmentGroupId = installmentGroupId,
            installmentsCount = installmentsCount,
            currentInstallment = currentInstallment,
            notes = notes
        )
    }

    private fun InvestmentEntity.toDomain(): Investment {
        return Investment(
            id = id,
            name = name,
            type = InvestmentType.valueOf(type),
            horizon = InvestmentHorizon.valueOf(horizon),
            currentBalanceInCents = currentBalanceInCents
        )
    }

    private fun Investment.toEntity(): InvestmentEntity {
        return InvestmentEntity(
            id = id,
            name = name,
            type = type.name,
            horizon = horizon.name,
            currentBalanceInCents = currentBalanceInCents
        )
    }
}
