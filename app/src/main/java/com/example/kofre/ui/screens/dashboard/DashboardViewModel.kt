package com.example.kofre.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kofre.domain.model.Transaction
import com.example.kofre.domain.repository.FinanceRepository
import com.example.kofre.domain.usecase.report.GetFinancialReportUseCase
import com.example.kofre.domain.usecase.report.PeriodType
import com.example.kofre.domain.usecase.report.TimeFilter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val totalIncomeInCents: Long = 0L,
    val totalExpenseInCents: Long = 0L,
    val netBalanceInCents: Long = 0L,
    val totalInvestedInCents: Long = 0L,
    val freeCashInCents: Long = 0L,
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false
)

class DashboardViewModel(
    private val repository: FinanceRepository,
    private val getFinancialReportUseCase: GetFinancialReportUseCase
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        getFinancialReportUseCase(TimeFilter(PeriodType.MONTH, System.currentTimeMillis())),
        repository.getAllTransactions()
    ) { report, transactions ->
        DashboardUiState(
            totalIncomeInCents = report.totalIncomeInCents,
            totalExpenseInCents = report.totalExpenseInCents,
            netBalanceInCents = report.netBalanceInCents,
            totalInvestedInCents = report.totalInvestedInCents,
            freeCashInCents = report.freeCashInCents,
            recentTransactions = transactions.sortedByDescending { it.timestamp }.take(5),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )
}
