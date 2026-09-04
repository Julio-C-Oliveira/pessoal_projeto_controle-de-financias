package com.example.kofre.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.RecurrenceFrequency
import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.model.RecurringTransaction
import com.example.kofre.domain.model.Transaction
import com.example.kofre.domain.repository.FinanceRepository
import com.example.kofre.domain.usecase.recurring.CreateRecurringTransactionParams
import com.example.kofre.domain.usecase.recurring.CreateRecurringTransactionUseCase
import com.example.kofre.domain.usecase.recurring.DeleteRecurringTransactionUseCase
import com.example.kofre.domain.usecase.recurring.GetRecurringTransactionsUseCase
import com.example.kofre.domain.usecase.recurring.ProcessDueRecurringTransactionsUseCase
import com.example.kofre.domain.usecase.recurring.ToggleRecurringTransactionUseCase
import com.example.kofre.domain.usecase.transaction.CreateTransactionParams
import com.example.kofre.domain.usecase.transaction.CreateTransactionUseCase
import com.example.kofre.domain.usecase.transaction.DeleteTransactionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val recurringTransactions: List<RecurringTransaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class TransactionsViewModel(
    private val repository: FinanceRepository,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getRecurringTransactionsUseCase: GetRecurringTransactionsUseCase? = null,
    private val createRecurringTransactionUseCase: CreateRecurringTransactionUseCase? = null,
    private val toggleRecurringTransactionUseCase: ToggleRecurringTransactionUseCase? = null,
    private val deleteRecurringTransactionUseCase: DeleteRecurringTransactionUseCase? = null,
    private val processDueRecurringTransactionsUseCase: ProcessDueRecurringTransactionsUseCase? = null
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)

    init {
        processDueTransactions()
    }

    fun processDueTransactions() {
        viewModelScope.launch {
            processDueRecurringTransactionsUseCase?.invoke(System.currentTimeMillis())
        }
    }

    val uiState: StateFlow<TransactionsUiState> = combine(
        repository.getAllTransactions(),
        getRecurringTransactionsUseCase?.invoke() ?: repository.getRecurringTransactions(),
        repository.getAllCategories(),
        _errorMessage
    ) { transactions, recurring, categories, error ->
        TransactionsUiState(
            transactions = transactions.sortedByDescending { it.timestamp },
            recurringTransactions = recurring,
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
        timestamp: Long = System.currentTimeMillis(),
        notes: String? = null
    ): Boolean {
        _errorMessage.value = null
        val params = CreateTransactionParams(
            amountInCents = amountInCents,
            timestamp = timestamp,
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

    suspend fun createRecurringTransaction(
        amountInCents: Long,
        categoryId: Long,
        type: TransactionType,
        paymentMethod: PaymentMethod,
        frequency: RecurrenceFrequency,
        isEssential: Boolean,
        startDate: Long = System.currentTimeMillis(),
        endDate: Long? = null,
        totalOccurrences: Int? = null,
        notes: String? = null
    ): Boolean {
        if (createRecurringTransactionUseCase == null) return false
        _errorMessage.value = null
        val params = CreateRecurringTransactionParams(
            amountInCents = amountInCents,
            categoryId = categoryId,
            type = type,
            paymentMethod = paymentMethod,
            frequency = frequency,
            startDate = startDate,
            endDate = endDate,
            totalOccurrences = totalOccurrences,
            isEssential = isEssential,
            notes = notes
        )
        val result = createRecurringTransactionUseCase(params)
        return if (result.isSuccess) {
            processDueTransactions()
            true
        } else {
            _errorMessage.value = result.exceptionOrNull()?.message ?: "Erro ao criar regra de recorrência."
            false
        }
    }

    suspend fun toggleRecurringTransaction(recurringId: Long, isActive: Boolean) {
        if (toggleRecurringTransactionUseCase == null) return
        _errorMessage.value = null
        val result = toggleRecurringTransactionUseCase(recurringId, isActive)
        if (result.isSuccess && isActive) {
            processDueTransactions()
        } else if (result.isFailure) {
            _errorMessage.value = result.exceptionOrNull()?.message ?: "Erro ao alterar estado da recorrência."
        }
    }

    suspend fun deleteRecurringTransactionRule(recurringId: Long) {
        if (deleteRecurringTransactionUseCase == null) return
        _errorMessage.value = null
        val result = deleteRecurringTransactionUseCase(recurringId)
        if (result.isFailure) {
            _errorMessage.value = result.exceptionOrNull()?.message ?: "Erro ao excluir regra de recorrência."
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
