package com.example.kofre.domain.usecase.transaction

import com.example.kofre.data.local.backup.BackupPayloadDto
import com.example.kofre.data.local.backup.CategoryBackupDto
import com.example.kofre.data.local.backup.InvestmentBackupDto
import com.example.kofre.data.local.backup.InvestmentContributionBackupDto
import com.example.kofre.data.local.backup.MonthlyBudgetBackupDto
import com.example.kofre.data.local.backup.TransactionBackupDto
import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.model.Investment
import com.example.kofre.domain.model.InvestmentContribution
import com.example.kofre.domain.model.MonthlyBudget
import com.example.kofre.domain.model.Transaction
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeFinanceRepository : FinanceRepository {

    private val categories = mutableListOf<Category>()
    private val transactions = mutableListOf<Transaction>()
    private val investments = mutableListOf<Investment>()
    private val contributions = mutableListOf<InvestmentContribution>()
    private val budgets = mutableListOf<MonthlyBudget>()

    private var nextCategoryId = 1L
    private var nextTransactionId = 1L
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
                notes = it.notes
            )
        }
        val invDtos = investments.map { InvestmentBackupDto(it.id, it.name, it.type.name, it.horizon.name, it.currentBalanceInCents) }
        val contribDtos = contributions.map { InvestmentContributionBackupDto(it.id, it.investmentId, it.amountInCents, it.timestamp, it.notes) }
        val budgetDtos = budgets.map { MonthlyBudgetBackupDto(it.id, it.year, it.month, it.categoryId, it.plannedAmountInCents) }

        return BackupPayloadDto(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            categories = catDtos,
            transactions = txDtos,
            investments = invDtos,
            investmentContributions = contribDtos,
            monthlyBudgets = budgetDtos
        )
    }

    override suspend fun importBackup(payload: BackupPayloadDto) {
        categories.clear()
        transactions.clear()
        investments.clear()
        contributions.clear()
        budgets.clear()

        payload.categories.forEach { dto ->
            categories.add(Category(dto.id, dto.name, CategoryType.valueOf(dto.type), dto.parentId))
        }
        payload.investments.forEach { dto ->
            investments.add(Investment(dto.id, dto.name, InvestmentType.valueOf(dto.type), InvestmentHorizon.valueOf(dto.horizon), dto.currentBalanceInCents))
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

