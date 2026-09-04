package com.example.kofre.domain.usecase.budget

import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

data class CategoryBudgetComparison(
    val categoryId: Long,
    val categoryName: String,
    val plannedAmountInCents: Long,
    val actualSpentInCents: Long,
    val remainingInCents: Long,
    val percentageUsed: Double,
    val isExceeded: Boolean
)

data class MonthlyBudgetOverview(
    val year: Int,
    val month: Int,
    val totalPlannedInCents: Long,
    val totalSpentInCents: Long,
    val totalRemainingInCents: Long,
    val categoriesComparison: List<CategoryBudgetComparison>
)

interface GetMonthlyBudgetOverviewUseCase {
    operator fun invoke(year: Int, month: Int): Flow<MonthlyBudgetOverview>
}

class GetMonthlyBudgetOverviewUseCaseImpl(
    private val repository: FinanceRepository
) : GetMonthlyBudgetOverviewUseCase {

    override operator fun invoke(year: Int, month: Int): Flow<MonthlyBudgetOverview> {
        val (startDate, endDate) = getMonthDateRange(year, month)

        return combine(
            repository.getBudgetsForMonth(year, month),
            repository.getAllCategories(),
            repository.getTransactionsByDateRange(startDate, endDate)
        ) { budgets, categories, transactions ->
            val categoryMap = flattenCategories(categories).associateBy { it.id }

            val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }
            val spentByCategory = expenseTransactions
                .groupBy { it.categoryId }
                .mapValues { entry -> entry.value.sumOf { it.amountInCents } }

            var totalPlanned = 0L
            var totalSpent = 0L

            val comparisons = budgets.map { budget ->
                val actualSpent = spentByCategory[budget.categoryId] ?: 0L
                val remaining = budget.plannedAmountInCents - actualSpent
                val percentage = if (budget.plannedAmountInCents > 0) {
                    (actualSpent.toDouble() / budget.plannedAmountInCents.toDouble()) * 100.0
                } else 0.0
                val isExceeded = actualSpent > budget.plannedAmountInCents

                totalPlanned += budget.plannedAmountInCents
                totalSpent += actualSpent

                CategoryBudgetComparison(
                    categoryId = budget.categoryId,
                    categoryName = categoryMap[budget.categoryId]?.name ?: "Desconhecido",
                    plannedAmountInCents = budget.plannedAmountInCents,
                    actualSpentInCents = actualSpent,
                    remainingInCents = remaining,
                    percentageUsed = percentage,
                    isExceeded = isExceeded
                )
            }

            MonthlyBudgetOverview(
                year = year,
                month = month,
                totalPlannedInCents = totalPlanned,
                totalSpentInCents = totalSpent,
                totalRemainingInCents = totalPlanned - totalSpent,
                categoriesComparison = comparisons
            )
        }
    }

    private fun getMonthDateRange(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    private fun flattenCategories(categories: List<Category>): List<Category> {
        val list = mutableListOf<Category>()
        for (cat in categories) {
            list.add(cat)
            if (cat.subcategories.isNotEmpty()) {
                list.addAll(flattenCategories(cat.subcategories))
            }
        }
        return list
    }
}
