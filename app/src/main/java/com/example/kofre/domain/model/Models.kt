package com.example.kofre.domain.model

import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.RecurrenceFrequency
import com.example.kofre.data.local.enums.TransactionType

data class Category(
    val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val parentId: Long? = null,
    val subcategories: List<Category> = emptyList()
)

data class Transaction(
    val id: Long = 0,
    val amountInCents: Long,
    val timestamp: Long,
    val categoryId: Long,
    val category: Category? = null,
    val type: TransactionType,
    val paymentMethod: PaymentMethod,
    val isEssential: Boolean = false,
    val installmentGroupId: String? = null,
    val installmentsCount: Int = 1,
    val currentInstallment: Int = 1,
    val recurringTransactionId: Long? = null,
    val notes: String? = null
)

data class RecurringTransaction(
    val id: Long = 0,
    val amountInCents: Long,
    val categoryId: Long,
    val category: Category? = null,
    val type: TransactionType,
    val paymentMethod: PaymentMethod,
    val frequency: RecurrenceFrequency,
    val startDate: Long,
    val endDate: Long? = null,
    val totalOccurrences: Int? = null,
    val generatedCount: Int = 0,
    val lastGeneratedDate: Long? = null,
    val isActive: Boolean = true,
    val isEssential: Boolean = false,
    val notes: String? = null
)

data class Investment(
    val id: Long = 0,
    val name: String,
    val type: InvestmentType,
    val horizon: InvestmentHorizon,
    val currentBalanceInCents: Long = 0L
)

data class InvestmentContribution(
    val id: Long = 0,
    val investmentId: Long,
    val amountInCents: Long,
    val timestamp: Long,
    val notes: String? = null
)

data class InvestmentSummary(
    val totalInvestedInCents: Long,
    val totalContributionsInCents: Long,
    val totalYieldInCents: Long,
    val investments: List<InvestmentDetail>
)

data class InvestmentDetail(
    val id: Long,
    val name: String,
    val type: InvestmentType,
    val horizon: InvestmentHorizon,
    val currentBalanceInCents: Long,
    val totalAportadoInCents: Long
)

data class MonthlyBudget(
    val id: Long = 0,
    val year: Int,
    val month: Int,
    val categoryId: Long,
    val plannedAmountInCents: Long
)

