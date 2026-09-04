package com.example.kofre.domain.usecase.budget

import com.example.kofre.domain.model.MonthlyBudget
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.first

interface CopyBudgetUseCase {
    suspend operator fun invoke(
        fromYear: Int,
        fromMonth: Int,
        toYear: Int,
        toMonth: Int,
        overrideExisting: Boolean = false
    ): Result<Int>
}

class CopyBudgetUseCaseImpl(
    private val repository: FinanceRepository
) : CopyBudgetUseCase {

    override suspend operator fun invoke(
        fromYear: Int,
        fromMonth: Int,
        toYear: Int,
        toMonth: Int,
        overrideExisting: Boolean
    ): Result<Int> {
        return runCatching {
            val sourceBudgets = repository.getBudgetsForMonth(fromYear, fromMonth).first()
            if (sourceBudgets.isEmpty()) {
                return@runCatching 0
            }

            val destBudgets = repository.getBudgetsForMonth(toYear, toMonth).first()
            val existingCategoryIds = destBudgets.map { it.categoryId }.toSet()

            val budgetsToProcess = if (overrideExisting) {
                sourceBudgets
            } else {
                sourceBudgets.filter { it.categoryId !in existingCategoryIds }
            }

            if (budgetsToProcess.isEmpty()) {
                return@runCatching 0
            }

            val newBudgets = budgetsToProcess.map { source ->
                val existingId = if (overrideExisting) {
                    destBudgets.find { it.categoryId == source.categoryId }?.id ?: 0L
                } else 0L

                MonthlyBudget(
                    id = existingId,
                    year = toYear,
                    month = toMonth,
                    categoryId = source.categoryId,
                    plannedAmountInCents = source.plannedAmountInCents
                )
            }

            repository.insertBudgets(newBudgets)
            newBudgets.size
        }
    }
}
