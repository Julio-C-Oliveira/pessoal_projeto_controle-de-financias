package com.example.kofre.domain.usecase.recurring

import com.example.kofre.data.local.entity.RecurringTransactionEntity
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.first

interface ToggleRecurringTransactionUseCase {
    suspend operator fun invoke(recurringId: Long, isActive: Boolean): Result<Unit>
}

class ToggleRecurringTransactionUseCaseImpl(
    private val repository: FinanceRepository
) : ToggleRecurringTransactionUseCase {

    override suspend operator fun invoke(recurringId: Long, isActive: Boolean): Result<Unit> {
        return try {
            val list = repository.getRecurringTransactions().first()
            val item = list.find { it.id == recurringId }
                ?: return Result.failure(IllegalArgumentException("Transação recorrente não encontrada."))

            val updatedEntity = RecurringTransactionEntity(
                id = item.id,
                amountInCents = item.amountInCents,
                categoryId = item.categoryId,
                type = item.type.name,
                paymentMethod = item.paymentMethod.name,
                frequency = item.frequency.name,
                startDate = item.startDate,
                lastGeneratedDate = item.lastGeneratedDate,
                isActive = isActive,
                isEssential = item.isEssential,
                notes = item.notes
            )
            repository.updateRecurringTransaction(updatedEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
