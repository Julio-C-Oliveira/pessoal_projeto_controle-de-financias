package com.example.kofre.domain.usecase.transaction

import com.example.kofre.domain.model.Transaction
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow

interface GetTransactionsUseCase {
    operator fun invoke(startDate: Long, endDate: Long): Flow<List<Transaction>>
}

class GetTransactionsUseCaseImpl(
    private val repository: FinanceRepository
) : GetTransactionsUseCase {

    override operator fun invoke(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return repository.getTransactionsByDateRange(startDate, endDate)
    }
}
