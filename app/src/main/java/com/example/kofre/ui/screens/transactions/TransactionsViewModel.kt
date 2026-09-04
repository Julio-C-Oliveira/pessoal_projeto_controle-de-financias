package com.example.kofre.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.model.Transaction
import com.example.kofre.domain.repository.FinanceRepository
import com.example.kofre.domain.usecase.transaction.CreateTransactionParams
import com.example.kofre.domain.usecase.transaction.CreateTransactionUseCase
import com.example.kofre.domain.usecase.transaction.DeleteTransactionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class TransactionsViewModel(
    private val repository: FinanceRepository,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TransactionsUiState> = combine(
        repository.getAllTransactions(),
        repository.getAllCategories(),
        _errorMessage
    ) { transactions, categories, error ->
        TransactionsUiState(
            transactions = transactions.sortedByDescending { it.timestamp },
            categories = categories,
            isLoading = false,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState(isLoading = true)
    )

    suspend fun createTransaction(
        amountInCents: Long,
        categoryId: Long,
        type: TransactionType,
        paymentMethod: PaymentMethod,
        isEssential: Boolean,
        installmentsCount: Int = 1,
        notes: String? = null
    ): Boolean {
        _errorMessage.value = null
        val params = CreateTransactionParams(
            amountInCents = amountInCents,
            timestamp = System.currentTimeMillis(),
            categoryId = categoryId,
            type = type,
            paymentMethod = paymentMethod,
            isEssential = isEssential,
            installmentsCount = installmentsCount,
            notes = notes
        )
        val result = createTransactionUseCase(params)
        return if (result.isSuccess) {
            true
        } else {
            _errorMessage.value = result.exceptionOrNull()?.message ?: "Erro ao criar transação."
            false
        }
    }

    suspend fun deleteTransaction(transactionId: Long, deleteGroup: Boolean = false) {
        _errorMessage.value = null
        val result = deleteTransactionUseCase(transactionId, deleteGroup)
        if (result.isFailure) {
            _errorMessage.value = result.exceptionOrNull()?.message ?: "Erro ao excluir transação."
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
