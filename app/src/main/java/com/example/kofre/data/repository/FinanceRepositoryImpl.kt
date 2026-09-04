package com.example.kofre.data.repository

import com.example.kofre.data.local.dao.CategoryDao
import com.example.kofre.data.local.dao.InvestmentContributionDao
import com.example.kofre.data.local.dao.InvestmentDao
import com.example.kofre.data.local.dao.MonthlyBudgetDao
import com.example.kofre.data.local.dao.RecurringTransactionDao
import com.example.kofre.data.local.dao.TransactionDao
import com.example.kofre.data.local.entity.CategoryEntity
import com.example.kofre.data.local.entity.InvestmentContributionEntity
import com.example.kofre.data.local.entity.InvestmentEntity
import com.example.kofre.data.local.entity.MonthlyBudgetEntity
import com.example.kofre.data.local.entity.RecurringTransactionEntity
import com.example.kofre.data.local.entity.TransactionEntity
import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.RecurrenceFrequency
import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.model.Investment
import com.example.kofre.domain.model.InvestmentContribution
import com.example.kofre.domain.model.MonthlyBudget
import com.example.kofre.domain.model.RecurringTransaction
import com.example.kofre.domain.model.Transaction
import com.example.kofre.domain.repository.FinanceRepository
import androidx.room.withTransaction
import com.example.kofre.data.local.AppDatabase
import com.example.kofre.data.local.backup.BackupPayloadDto
import com.example.kofre.data.local.backup.toBackupDto
import com.example.kofre.data.local.backup.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FinanceRepositoryImpl(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val investmentDao: InvestmentDao,
    private val investmentContributionDao: InvestmentContributionDao? = null,
    private val monthlyBudgetDao: MonthlyBudgetDao? = null,
    private val recurringTransactionDao: RecurringTransactionDao? = null,
    private val database: AppDatabase? = null
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
        ) { transactions: List<TransactionEntity>, categories: List<CategoryEntity> ->
            val categoryMap = categories.associateBy { it.id }
            transactions.map { it.toDomain(categoryMap[it.categoryId]?.toDomainFlat()) }
        }
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return combine(
            transactionDao.getTransactionsByDateRange(startDate, endDate),
            categoryDao.getAllCategories()
        ) { transactions: List<TransactionEntity>, categories: List<CategoryEntity> ->
            val categoryMap = categories.associateBy { it.id }
            transactions.map { it.toDomain(categoryMap[it.categoryId]?.toDomainFlat()) }
        }
    }

    override fun getTransactionsByGroupId(groupId: String): Flow<List<Transaction>> {
        return combine(
            transactionDao.getTransactionsByGroupId(groupId),
            categoryDao.getAllCategories()
        ) { transactions: List<TransactionEntity>, categories: List<CategoryEntity> ->
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

    override fun getRecurringTransactions(): Flow<List<RecurringTransaction>> {
        val dao = recurringTransactionDao ?: return flowOf(emptyList())
        return combine(
            dao.getAllRecurringTransactions(),
            categoryDao.getAllCategories()
        ) { recurringList: List<RecurringTransactionEntity>, categories: List<CategoryEntity> ->
            val categoryMap = categories.associateBy { it.id }
            recurringList.map { it.toDomain(categoryMap[it.categoryId]?.toDomainFlat()) }
        }
    }

    override suspend fun insertRecurringTransaction(recurring: RecurringTransactionEntity): Long {
        return recurringTransactionDao?.insert(recurring) ?: 0L
    }

    override suspend fun updateRecurringTransaction(recurring: RecurringTransactionEntity) {
        recurringTransactionDao?.update(recurring)
    }

    override suspend fun deleteRecurringTransaction(id: Long) {
        recurringTransactionDao?.deleteById(id)
    }

    override suspend fun getActiveRecurringEntities(): List<RecurringTransactionEntity> {
        return recurringTransactionDao?.getActiveRecurringEntities() ?: emptyList()
    }

    override fun getAllInvestments(): Flow<List<Investment>> {
        return investmentDao.getAllInvestments().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getInvestmentById(id: Long): Flow<Investment?> {
        return investmentDao.getAllInvestments().map { entities ->
            entities.find { it.id == id }?.toDomain()
        }
    }

    override suspend fun insertInvestment(investment: Investment): Long {
        return investmentDao.insertInvestment(investment.toEntity())
    }

    override suspend fun updateInvestmentBalance(id: Long, newBalanceInCents: Long) {
        investmentDao.updateBalance(id, newBalanceInCents)
    }

    override suspend fun deleteInvestment(id: Long) {
        investmentDao.deleteById(id)
    }

    override fun getAllContributions(): Flow<List<InvestmentContribution>> {
        return investmentContributionDao?.getAllContributions()?.map { entities ->
            entities.map { it.toDomain() }
        } ?: flowOf(emptyList())
    }

    override fun getContributionsByInvestmentId(investmentId: Long): Flow<List<InvestmentContribution>> {
        return investmentContributionDao?.getContributionsByInvestmentId(investmentId)?.map { entities ->
            entities.map { it.toDomain() }
        } ?: flowOf(emptyList())
    }

    override suspend fun insertContribution(contribution: InvestmentContribution): Long {
        return investmentContributionDao?.insertContribution(contribution.toEntity()) ?: 0L
    }

    override fun getBudgetsForMonth(year: Int, month: Int): Flow<List<MonthlyBudget>> {
        return monthlyBudgetDao?.getBudgetsForMonth(year, month)?.map { entities ->
            entities.map { it.toDomain() }
        } ?: flowOf(emptyList())
    }

    override fun getBudget(year: Int, month: Int, categoryId: Long): Flow<MonthlyBudget?> {
        return monthlyBudgetDao?.getBudget(year, month, categoryId)?.map { entity ->
            entity?.toDomain()
        } ?: flowOf(null)
    }

    override suspend fun insertBudget(budget: MonthlyBudget): Long {
        return monthlyBudgetDao?.upsertBudget(budget.toEntity()) ?: 0L
    }

    override suspend fun insertBudgets(budgets: List<MonthlyBudget>): List<Long> {
        return monthlyBudgetDao?.upsertBudgets(budgets.map { it.toEntity() }) ?: emptyList()
    }

    override suspend fun deleteBudget(budgetId: Long) {
        monthlyBudgetDao?.deleteBudgetById(budgetId)
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
            recurringTransactionId = recurringTransactionId,
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
            recurringTransactionId = recurringTransactionId,
            notes = notes
        )
    }

    private fun RecurringTransactionEntity.toDomain(category: Category?): RecurringTransaction {
        return RecurringTransaction(
            id = id,
            amountInCents = amountInCents,
            categoryId = categoryId,
            category = category,
            type = TransactionType.valueOf(type),
            paymentMethod = PaymentMethod.valueOf(paymentMethod),
            frequency = RecurrenceFrequency.valueOf(frequency),
            startDate = startDate,
            endDate = endDate,
            totalOccurrences = totalOccurrences,
            generatedCount = generatedCount,
            lastGeneratedDate = lastGeneratedDate,
            isActive = isActive,
            isEssential = isEssential,
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

    private fun InvestmentContributionEntity.toDomain(): InvestmentContribution {
        return InvestmentContribution(
            id = id,
            investmentId = investmentId,
            amountInCents = amountInCents,
            timestamp = timestamp,
            notes = notes
        )
    }

    private fun InvestmentContribution.toEntity(): InvestmentContributionEntity {
        return InvestmentContributionEntity(
            id = id,
            investmentId = investmentId,
            amountInCents = amountInCents,
            timestamp = timestamp,
            notes = notes
        )
    }

    private fun MonthlyBudgetEntity.toDomain(): MonthlyBudget {
        return MonthlyBudget(
            id = id,
            year = year,
            month = month,
            categoryId = categoryId,
            plannedAmountInCents = plannedAmountInCents
        )
    }

    private fun MonthlyBudget.toEntity(): MonthlyBudgetEntity {
        return MonthlyBudgetEntity(
            id = id,
            year = year,
            month = month,
            categoryId = categoryId,
            plannedAmountInCents = plannedAmountInCents
        )
    }

    override suspend fun exportBackup(): BackupPayloadDto {
        val categories = categoryDao.getAllCategories().first().map { it.toBackupDto() }
        val transactions = transactionDao.getAllTransactions().first().map { it.toBackupDto() }
        val investments = investmentDao.getAllInvestments().first().map { it.toBackupDto() }
        val contributions = investmentContributionDao?.getAllContributions()?.first()?.map { it.toBackupDto() } ?: emptyList()
        val budgets = monthlyBudgetDao?.getAllBudgets()?.first()?.map { it.toBackupDto() } ?: emptyList()
        val recurring = recurringTransactionDao?.getAllRecurringTransactions()?.first()?.map { it.toBackupDto() } ?: emptyList()

        return BackupPayloadDto(
            version = 2,
            exportedAt = System.currentTimeMillis(),
            categories = categories,
            transactions = transactions,
            investments = investments,
            investmentContributions = contributions,
            monthlyBudgets = budgets,
            recurringTransactions = recurring
        )
    }

    override suspend fun importBackup(payload: BackupPayloadDto) {
        val performImport: suspend () -> Unit = {
            // 1. Wipe in reverse foreign key order
            monthlyBudgetDao?.deleteAllBudgets()
            investmentContributionDao?.deleteAllContributions()
            transactionDao.deleteAllTransactions()
            recurringTransactionDao?.deleteAllRecurringTransactions()
            investmentDao.deleteAllInvestments()
            categoryDao.deleteSubcategories()
            categoryDao.deleteAllCategories()

            // 2. Insert in foreign key order
            val (parents, children) = payload.categories.map { it.toEntity() }.partition { it.parentId == null }
            if (parents.isNotEmpty()) categoryDao.insertCategories(parents)
            if (children.isNotEmpty()) categoryDao.insertCategories(children)

            val investments = payload.investments.map { it.toEntity() }
            if (investments.isNotEmpty()) investmentDao.insertInvestments(investments)

            val recurring = payload.recurringTransactions.map { it.toEntity() }
            if (recurring.isNotEmpty()) recurringTransactionDao?.insertAll(recurring)

            val transactions = payload.transactions.map { it.toEntity() }
            if (transactions.isNotEmpty()) transactionDao.insertTransactions(transactions)

            val contributions = payload.investmentContributions.map { it.toEntity() }
            if (contributions.isNotEmpty()) investmentContributionDao?.insertContributions(contributions)

            val budgets = payload.monthlyBudgets.map { it.toEntity() }
            if (budgets.isNotEmpty()) monthlyBudgetDao?.upsertBudgets(budgets)
        }

        if (database != null) {
            database.withTransaction {
                performImport()
            }
        } else {
            performImport()
        }
    }
}


