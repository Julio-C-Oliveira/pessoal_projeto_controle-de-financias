package com.example.kofre.domain.usecase.report

import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class PeriodType { WEEK, MONTH, YEAR }

data class TimeFilter(
    val periodType: PeriodType,
    val referenceDate: Long // Timestamp em milissegundos
)

data class CategoryExpenseSummary(
    val categoryId: Long,
    val categoryName: String,
    val totalInCents: Long,
    val percentageOfTotal: Double
)

data class PeriodFinancialReport(
    val filter: TimeFilter,
    val totalIncomeInCents: Long,
    val totalExpenseInCents: Long,
    val essentialExpenseInCents: Long,
    val nonEssentialExpenseInCents: Long,
    val netBalanceInCents: Long,
    val totalInvestedInCents: Long,
    val freeCashInCents: Long,
    val categoryExpenses: List<CategoryExpenseSummary>
)

interface GetFinancialReportUseCase {
    operator fun invoke(filter: TimeFilter): Flow<PeriodFinancialReport>
}

class GetFinancialReportUseCaseImpl(
    private val repository: FinanceRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : GetFinancialReportUseCase {

    override operator fun invoke(filter: TimeFilter): Flow<PeriodFinancialReport> {
        val (startMillis, endMillis) = calculateDateRange(filter.referenceDate, filter.periodType)

        return combine(
            repository.getTransactionsByDateRange(startMillis, endMillis),
            repository.getAllContributions(),
            repository.getAllCategories()
        ) { transactions, contributions, categories ->
            val incomeTransactions = transactions.filter { it.type == TransactionType.INCOME }
            val expenseTransactions = transactions.filter { it.type == TransactionType.EXPENSE }

            val totalIncomeInCents = incomeTransactions.sumOf { it.amountInCents }
            val totalExpenseInCents = expenseTransactions.sumOf { it.amountInCents }
            val essentialExpenseInCents = expenseTransactions.filter { it.isEssential }.sumOf { it.amountInCents }
            val nonEssentialExpenseInCents = expenseTransactions.filter { !it.isEssential }.sumOf { it.amountInCents }

            val netBalanceInCents = totalIncomeInCents - totalExpenseInCents

            val periodContributions = contributions.filter { it.timestamp in startMillis..endMillis }
            val totalInvestedInCents = periodContributions.sumOf { it.amountInCents }

            val freeCashInCents = netBalanceInCents - totalInvestedInCents

            val categoryMap = flattenCategories(categories).associateBy { it.id }
            val groupedExpenses = expenseTransactions.groupBy { it.categoryId }

            val categoryExpenses = groupedExpenses.map { (catId, catTxs) ->
                val totalCatInCents = catTxs.sumOf { it.amountInCents }
                val percentage = if (totalExpenseInCents > 0L) {
                    (totalCatInCents.toDouble() / totalExpenseInCents.toDouble()) * 100.0
                } else {
                    0.0
                }
                val catName = categoryMap[catId]?.name ?: catTxs.firstOrNull()?.category?.name ?: "Outros"
                CategoryExpenseSummary(
                    categoryId = catId,
                    categoryName = catName,
                    totalInCents = totalCatInCents,
                    percentageOfTotal = percentage
                )
            }.sortedByDescending { it.totalInCents }

            PeriodFinancialReport(
                filter = filter,
                totalIncomeInCents = totalIncomeInCents,
                totalExpenseInCents = totalExpenseInCents,
                essentialExpenseInCents = essentialExpenseInCents,
                nonEssentialExpenseInCents = nonEssentialExpenseInCents,
                netBalanceInCents = netBalanceInCents,
                totalInvestedInCents = totalInvestedInCents,
                freeCashInCents = freeCashInCents,
                categoryExpenses = categoryExpenses
            )
        }
    }

    private fun calculateDateRange(referenceDate: Long, periodType: PeriodType): Pair<Long, Long> {
        val localDate = Instant.ofEpochMilli(referenceDate).atZone(zoneId).toLocalDate()

        return when (periodType) {
            PeriodType.WEEK -> {
                val startOfDay = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay()
                val endOfDay = localDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(LocalTime.MAX)
                Pair(
                    startOfDay.atZone(zoneId).toInstant().toEpochMilli(),
                    endOfDay.atZone(zoneId).toInstant().toEpochMilli()
                )
            }
            PeriodType.MONTH -> {
                val startOfDay = localDate.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay()
                val endOfDay = localDate.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX)
                Pair(
                    startOfDay.atZone(zoneId).toInstant().toEpochMilli(),
                    endOfDay.atZone(zoneId).toInstant().toEpochMilli()
                )
            }
            PeriodType.YEAR -> {
                val startOfDay = localDate.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay()
                val endOfDay = localDate.with(TemporalAdjusters.lastDayOfYear()).atTime(LocalTime.MAX)
                Pair(
                    startOfDay.atZone(zoneId).toInstant().toEpochMilli(),
                    endOfDay.atZone(zoneId).toInstant().toEpochMilli()
                )
            }
        }
    }

    private fun flattenCategories(categories: List<Category>): List<Category> {
        val result = mutableListOf<Category>()
        for (cat in categories) {
            result.add(cat)
            if (cat.subcategories.isNotEmpty()) {
                result.addAll(flattenCategories(cat.subcategories))
            }
        }
        return result
    }
}
