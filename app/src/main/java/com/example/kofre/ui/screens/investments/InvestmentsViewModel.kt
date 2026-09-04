package com.example.kofre.ui.screens.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.domain.model.InvestmentSummary
import com.example.kofre.domain.repository.FinanceRepository
import com.example.kofre.domain.usecase.investment.AddContributionParams
import com.example.kofre.domain.usecase.investment.AddContributionUseCase
import com.example.kofre.domain.usecase.investment.CreateInvestmentParams
import com.example.kofre.domain.usecase.investment.CreateInvestmentUseCase
import com.example.kofre.domain.usecase.investment.GetInvestmentsSummaryUseCase
import com.example.kofre.domain.usecase.investment.UpdateInvestmentBalanceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class InvestmentsUiState(
    val summary: InvestmentSummary? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class InvestmentsViewModel(
    private val repository: FinanceRepository,
    private val getInvestmentsSummaryUseCase: GetInvestmentsSummaryUseCase,
    private val createInvestmentUseCase: CreateInvestmentUseCase,
    private val addContributionUseCase: AddContributionUseCase,
    private val updateInvestmentBalanceUseCase: UpdateInvestmentBalanceUseCase
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<InvestmentsUiState> = combine(
        getInvestmentsSummaryUseCase(),
        _errorMessage
    ) { summary, error ->
        InvestmentsUiState(
            summary = summary,
            isLoading = false,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InvestmentsUiState(isLoading = true)
    )

    suspend fun createInvestment(
        name: String,
        type: InvestmentType,
        horizon: InvestmentHorizon,
        initialBalanceInCents: Long = 0L
    ): Boolean {
        _errorMessage.value = null
        val params = CreateInvestmentParams(
            name = name,
            type = type,
            horizon = horizon,
            initialAmountInCents = initialBalanceInCents
        )
        val result = createInvestmentUseCase(params)
        return if (result.isSuccess) {
            true
        } else {
            _errorMessage.value = result.exceptionOrNull()?.message ?: "Erro ao criar investimento."
            false
        }
    }

    suspend fun addContribution(
        investmentId: Long,
        amountInCents: Long,
        notes: String? = null
    ): Boolean {
        _errorMessage.value = null
        val params = AddContributionParams(
            investmentId = investmentId,
            amountInCents = amountInCents,
            timestamp = System.currentTimeMillis(),
            notes = notes
        )
        val result = addContributionUseCase(params)
        return if (result.isSuccess) {
            true
        } else {
            _errorMessage.value = result.exceptionOrNull()?.message ?: "Erro ao registrar aporte."
            false
        }
    }

    suspend fun updateBalance(
        investmentId: Long,
        newBalanceInCents: Long
    ): Boolean {
        _errorMessage.value = null
        val result = updateInvestmentBalanceUseCase(investmentId, newBalanceInCents)
        return if (result.isSuccess) {
            true
        } else {
            _errorMessage.value = result.exceptionOrNull()?.message ?: "Erro ao atualizar saldo."
            false
        }
    }
}
