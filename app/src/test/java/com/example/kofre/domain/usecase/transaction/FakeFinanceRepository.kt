package com.example.kofre.domain.usecase.transaction

import com.example.kofre.data.local.backup.BackupPayloadDto
import com.example.kofre.data.local.backup.CategoryBackupDto
import com.example.kofre.data.local.backup.InvestmentBackupDto
import com.example.kofre.data.local.backup.InvestmentContributionBackupDto
import com.example.kofre.data.local.backup.MonthlyBudgetBackupDto
import com.example.kofre.data.local.backup.RecurringTransactionBackupDto
import com.example.kofre.data.local.backup.TransactionBackupDto
import com.example.kofre.data.local.entity.RecurringTransactionEntity
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeFinanceRepository : FinanceRepository {

    private val categories = mutableListOf<Category>()
    private val transactions = mutableListOf<Transaction>()
    private val recurringTransactions = mutableListOf<RecurringTransactionEntity>()
    private val investments = mutableListOf<Investment>()
    private val contributions = mutableListOf<InvestmentContribution>()
    private val budgets = mutableListOf<MonthlyBudget>()

    private var nextCategoryId = 1L
    private var nextTransactionId = 1L
    private var nextRecurringId = 1L
    private var nextInvestmentId = 1L
    private var nextContributionId = 1L
    private var nextBudgetId = 1L

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
        // Cascading delete budgets for this category
        budgets.removeAll { it.categoryId == category.id }
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

    override fun getRecurringTransactions(): Flow<List<RecurringTransaction>> {
        val domainList = recurringTransactions.map { entity ->
            val cat = categories.find { it.id == entity.categoryId }
            RecurringTransaction(
                id = entity.id,
                amountInCents = entity.amountInCents,
                categoryId = entity.categoryId,
                category = cat,
                type = TransactionType.valueOf(entity.type),
                paymentMethod = PaymentMethod.valueOf(entity.paymentMethod),
                frequency = RecurrenceFrequency.valueOf(entity.frequency),
                startDate = entity.startDate,
                endDate = entity.endDate,
                totalOccurrences = entity.totalOccurrences,
                generatedCount = entity.generatedCount,
                lastGeneratedDate = entity.lastGeneratedDate,
                isActive = entity.isActive,
                isEssential = entity.isEssential,
                notes = entity.notes
            )
        }
        return flowOf(domainList)
    }

    override suspend fun insertRecurringTransaction(recurring: RecurringTransactionEntity): Long {
        val id = if (recurring.id == 0L) nextRecurringId++ else recurring.id
        val newEntity = recurring.copy(id = id)
        recurringTransactions.add(newEntity)
        return id
    }

    override suspend fun updateRecurringTransaction(recurring: RecurringTransactionEntity) {
        val index = recurringTransactions.indexOfFirst { it.id == recurring.id }
        if (index != -1) {
            recurringTransactions[index] = recurring
        }
    }

    override suspend fun deleteRecurringTransaction(id: Long) {
        recurringTransactions.removeAll { it.id == id }
    }

    override suspend fun getActiveRecurringEntities(): List<RecurringTransactionEntity> {
        return recurringTransactions.filter { it.isActive }
    }

    override fun getAllInvestments(): Flow<List<Investment>> {
        return flowOf(investments.toList())
    }

    override fun getInvestmentById(id: Long): Flow<Investment?> {
        val inv = investments.find { it.id == id }
        return flowOf(inv)
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

    override suspend fun deleteInvestment(id: Long) {
        investments.removeAll { it.id == id }
    }

    override fun getAllContributions(): Flow<List<InvestmentContribution>> {
        return flowOf(contributions.sortedByDescending { it.timestamp })
    }

    override fun getContributionsByInvestmentId(investmentId: Long): Flow<List<InvestmentContribution>> {
        val filtered = contributions.filter { it.investmentId == investmentId }
            .sortedByDescending { it.timestamp }
        return flowOf(filtered)
    }

    override suspend fun insertContribution(contribution: InvestmentContribution): Long {
        val id = if (contribution.id == 0L) nextContributionId++ else contribution.id
        val newContrib = contribution.copy(id = id)
        contributions.add(newContrib)
        return id
    }

    override fun getBudgetsForMonth(year: Int, month: Int): Flow<List<MonthlyBudget>> {
        val filtered = budgets.filter { it.year == year && it.month == month }
        return flowOf(filtered)
    }

    override fun getBudget(year: Int, month: Int, categoryId: Long): Flow<MonthlyBudget?> {
        val b = budgets.find { it.year == year && it.month == month && it.categoryId == categoryId }
        return flowOf(b)
    }

    override suspend fun insertBudget(budget: MonthlyBudget): Long {
        val existingIndex = budgets.indexOfFirst {
            (budget.id > 0L && it.id == budget.id) ||
            (it.year == budget.year && it.month == budget.month && it.categoryId == budget.categoryId)
        }

        return if (existingIndex != -1) {
            val existing = budgets[existingIndex]
            val updated = budget.copy(id = existing.id)
            budgets[existingIndex] = updated
            existing.id
        } else {
            val id = if (budget.id == 0L) nextBudgetId++ else budget.id
            val newBudget = budget.copy(id = id)
            budgets.add(newBudget)
            id
        }
    }

    override suspend fun insertBudgets(budgetsList: List<MonthlyBudget>): List<Long> {
        return budgetsList.map { insertBudget(it) }
    }

    override suspend fun deleteBudget(budgetId: Long) {
        budgets.removeAll { it.id == budgetId }
    }

    override suspend fun exportBackup(): BackupPayloadDto {
        val catDtos = categories.map { CategoryBackupDto(it.id, it.name, it.type.name, it.parentId) }
        val txDtos = transactions.map {
            TransactionBackupDto(
                id = it.id,
                amountInCents = it.amountInCents,
                timestamp = it.timestamp,
                categoryId = it.categoryId,
                type = it.type.name,
                paymentMethod = it.paymentMethod.name,
                isEssential = it.isEssential,
                installmentGroupId = it.installmentGroupId,
                installmentsCount = it.installmentsCount,
                currentInstallment = it.currentInstallment,
                recurringTransactionId = it.recurringTransactionId,
                notes = it.notes
            )
        }
        val invDtos = investments.map { InvestmentBackupDto(it.id, it.name, it.type.name, it.horizon.name, it.currentBalanceInCents) }
        val contribDtos = contributions.map { InvestmentContributionBackupDto(it.id, it.investmentId, it.amountInCents, it.timestamp, it.notes) }
        val budgetDtos = budgets.map { MonthlyBudgetBackupDto(it.id, it.year, it.month, it.categoryId, it.plannedAmountInCents) }
        val recurringDtos = recurringTransactions.map {
            RecurringTransactionBackupDto(
                id = it.id,
                amountInCents = it.amountInCents,
                categoryId = it.categoryId,
                type = it.type,
                paymentMethod = it.paymentMethod,
                frequency = it.frequency,
                startDate = it.startDate,
                endDate = it.endDate,
                totalOccurrences = it.totalOccurrences,
                generatedCount = it.generatedCount,
                lastGeneratedDate = it.lastGeneratedDate,
                isActive = it.isActive,
                isEssential = it.isEssential,
                notes = it.notes
            )
        }

        return BackupPayloadDto(
            version = 2,
            exportedAt = System.currentTimeMillis(),
            categories = catDtos,
            transactions = txDtos,
            investments = invDtos,
            investmentContributions = contribDtos,
            monthlyBudgets = budgetDtos,
            recurringTransactions = recurringDtos
        )
    }

    override suspend fun importBackup(payload: BackupPayloadDto) {
        categories.clear()
        transactions.clear()
        recurringTransactions.clear()
        investments.clear()
        contributions.clear()
        budgets.clear()

        payload.categories.forEach { dto ->
            categories.add(Category(dto.id, dto.name, CategoryType.valueOf(dto.type), dto.parentId))
        }
        payload.investments.forEach { dto ->
            investments.add(Investment(dto.id, dto.name, InvestmentType.valueOf(dto.type), InvestmentHorizon.valueOf(dto.horizon), dto.currentBalanceInCents))
        }
        payload.recurringTransactions.forEach { dto ->
            recurringTransactions.add(
                RecurringTransactionEntity(
                    id = dto.id,
                    amountInCents = dto.amountInCents,
                    categoryId = dto.categoryId,
                    type = dto.type,
                    paymentMethod = dto.paymentMethod,
                    frequency = dto.frequency,
                    startDate = dto.startDate,
                    endDate = dto.endDate,
                    totalOccurrences = dto.totalOccurrences,
                    generatedCount = dto.generatedCount,
                    lastGeneratedDate = dto.lastGeneratedDate,
                    isActive = dto.isActive,
                    isEssential = dto.isEssential,
                    notes = dto.notes
                )
            )
        }
        payload.transactions.forEach { dto ->
            val cat = categories.find { c -> c.id == dto.categoryId }
            transactions.add(
                Transaction(
                    id = dto.id,
                    amountInCents = dto.amountInCents,
                    timestamp = dto.timestamp,
                    categoryId = dto.categoryId,
                    category = cat,
                    type = TransactionType.valueOf(dto.type),
                    paymentMethod = PaymentMethod.valueOf(dto.paymentMethod),
                    isEssential = dto.isEssential,
                    installmentGroupId = dto.installmentGroupId,
                    installmentsCount = dto.installmentsCount,
                    currentInstallment = dto.currentInstallment,
                    recurringTransactionId = dto.recurringTransactionId,
                    notes = dto.notes
                )
            )
        }
        payload.investmentContributions.forEach { dto ->
            contributions.add(InvestmentContribution(dto.id, dto.investmentId, dto.amountInCents, dto.timestamp, dto.notes))
        }
        payload.monthlyBudgets.forEach { dto ->
            budgets.add(MonthlyBudget(dto.id, dto.year, dto.month, dto.categoryId, dto.plannedAmountInCents))
        }
    }
}

