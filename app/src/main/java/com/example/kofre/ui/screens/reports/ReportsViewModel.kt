package com.example.kofre.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kofre.domain.usecase.report.GetFinancialReportUseCase
import com.example.kofre.domain.usecase.report.PeriodFinancialReport
import com.example.kofre.domain.usecase.report.PeriodType
import com.example.kofre.domain.usecase.report.TimeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId

data class ReportsUiState(
    val filter: TimeFilter = TimeFilter(PeriodType.MONTH, System.currentTimeMillis()),
    val report: PeriodFinancialReport? = null,
    val isLoading: Boolean = false
)

class ReportsViewModel(
    private val getFinancialReportUseCase: GetFinancialReportUseCase
) : ViewModel() {

    private val _filter = MutableStateFlow(TimeFilter(PeriodType.MONTH, System.currentTimeMillis()))

    val uiState: StateFlow<ReportsUiState> = _filter.flatMapLatest { filter ->
        combine(
            getFinancialReportUseCase(filter)
        ) { (report) ->
            ReportsUiState(
                filter = filter,
                report = report,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState(isLoading = true)
    )

    fun selectPeriodType(periodType: PeriodType) {
        _filter.value = _filter.value.copy(periodType = periodType)
    }

    fun navigatePeriod(forward: Boolean) {
        val current = Instant.ofEpochMilli(_filter.value.referenceDate).atZone(ZoneId.systemDefault())
        val newZdt = when (_filter.value.periodType) {
            PeriodType.WEEK -> if (forward) current.plusWeeks(1) else current.minusWeeks(1)
            PeriodType.MONTH -> if (forward) current.plusMonths(1) else current.minusMonths(1)
            PeriodType.YEAR -> if (forward) current.plusYears(1) else current.minusYears(1)
        }
        _filter.value = _filter.value.copy(referenceDate = newZdt.toInstant().toEpochMilli())
    }
}
