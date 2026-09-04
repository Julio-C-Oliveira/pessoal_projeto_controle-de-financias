package com.example.kofre.domain.usecase.budget

import com.example.kofre.domain.repository.FinanceRepository

interface RemoveCategoryBudgetUseCase {
    suspend operator fun invoke(budgetId: Long): Result<Unit>
}

class RemoveCategoryBudgetUseCaseImpl(
    private val repository: FinanceRepository
) : RemoveCategoryBudgetUseCase {

    override suspend operator fun invoke(budgetId: Long): Result<Unit> {
        return runCatching {
            repository.deleteBudget(budgetId)
        }
    }
}
