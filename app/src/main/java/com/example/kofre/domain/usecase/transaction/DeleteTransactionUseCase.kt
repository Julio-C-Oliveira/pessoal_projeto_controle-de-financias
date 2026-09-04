package com.example.kofre.domain.usecase.transaction

import com.example.kofre.domain.model.TransactionNotFoundException
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.first

interface DeleteTransactionUseCase {
    suspend operator fun invoke(
        transactionId: Long,
        deleteEntireGroup: Boolean = false
    ): Result<Unit>
}

class DeleteTransactionUseCaseImpl(
    private val repository: FinanceRepository
) : DeleteTransactionUseCase {

    override suspend operator fun invoke(
        transactionId: Long,
        deleteEntireGroup: Boolean
    ): Result<Unit> {
        return runCatching {
            val transactions = repository.getAllTransactions().first()
            val transaction = transactions.find { it.id == transactionId }
                ?: throw TransactionNotFoundException()

            if (deleteEntireGroup && transaction.installmentGroupId != null) {
                repository.deleteTransactionsByGroupId(transaction.installmentGroupId)
            } else {
                repository.deleteTransaction(transaction)
            }
        }
    }
}
