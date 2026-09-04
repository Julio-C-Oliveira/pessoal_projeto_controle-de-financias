package com.example.kofre.ui.screens.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.repository.FinanceRepository
import com.example.kofre.domain.usecase.budget.GetMonthlyBudgetOverviewUseCase
import com.example.kofre.domain.usecase.budget.MonthlyBudgetOverview
import com.example.kofre.domain.usecase.budget.SetBudgetParams
import com.example.kofre.domain.usecase.budget.SetCategoryBudgetUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class BudgetUiState(
    val year: Int = LocalDate.now().year,
    val month: Int = LocalDate.now().monthValue,
    val overview: MonthlyBudgetOverview? = null,
    val expenseCategories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class BudgetViewModel(
    private val repository: FinanceRepository,
    private val getMonthlyBudgetOverviewUseCase: GetMonthlyBudgetOverviewUseCase,
    private val setCategoryBudgetUseCase: SetCategoryBudgetUseCase
) : ViewModel() {

    private val _year = MutableStateFlow(LocalDate.now().year)
    private val _month = MutableStateFlow(LocalDate.now().monthValue)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BudgetUiState> = combine(_year, _month) { year, month ->
        Pair(year, month)
    }.flatMapLatest { (year, month) ->
        combine(
            getMonthlyBudgetOverviewUseCase(year, month),
            repository.getAllCategories(),
            _errorMessage
        ) { overview, categories, error ->
            BudgetUiState(
                year = year,
                month = month,
                overview = overview,
                expenseCategories = categories.filter { it.type.name == "EXPENSE" },
                isLoading = false,
                errorMessage = error
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetUiState(isLoading = true)
    )

    fun changeMonth(year: Int, month: Int) {
        _year.value = year
        _month.value = month
    }

    suspend fun setBudget(categoryId: Long, amountInCents: Long): Boolean {
        _errorMessage.value = null
        val params = SetBudgetParams(
            year = _year.value,
            month = _month.value,
            categoryId = categoryId,
            plannedAmountInCents = amountInCents
        )
        val result = setCategoryBudgetUseCase(params)
        return if (result.isSuccess) {
            true
        } else {
            _errorMessage.value = result.exceptionOrNull()?.message ?: "Erro ao definir orçamento."
            false
        }
    }
}
