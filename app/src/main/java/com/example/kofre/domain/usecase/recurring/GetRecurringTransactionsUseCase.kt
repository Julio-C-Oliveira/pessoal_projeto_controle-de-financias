package com.example.kofre.domain.usecase.recurring

import com.example.kofre.domain.model.RecurringTransaction
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow

interface GetRecurringTransactionsUseCase {
    operator fun invoke(): Flow<List<RecurringTransaction>>
}

class GetRecurringTransactionsUseCaseImpl(
    private val repository: FinanceRepository
) : GetRecurringTransactionsUseCase {
    override operator fun invoke(): Flow<List<RecurringTransaction>> {
        return repository.getRecurringTransactions()
    }
}
