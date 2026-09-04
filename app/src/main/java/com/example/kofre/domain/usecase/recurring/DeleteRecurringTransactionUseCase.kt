package com.example.kofre.domain.usecase.recurring

import com.example.kofre.domain.repository.FinanceRepository

interface DeleteRecurringTransactionUseCase {
    suspend operator fun invoke(recurringId: Long): Result<Unit>
}

class DeleteRecurringTransactionUseCaseImpl(
    private val repository: FinanceRepository
) : DeleteRecurringTransactionUseCase {

    override suspend operator fun invoke(recurringId: Long): Result<Unit> {
        return try {
            repository.deleteRecurringTransaction(recurringId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
