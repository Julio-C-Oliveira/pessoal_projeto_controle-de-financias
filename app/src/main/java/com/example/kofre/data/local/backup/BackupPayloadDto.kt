package com.example.kofre.data.local.backup

import kotlinx.serialization.Serializable

@Serializable
data class CategoryBackupDto(
    val id: Long,
    val name: String,
    val type: String,
    val parentId: Long? = null
)

@Serializable
data class RecurringTransactionBackupDto(
    val id: Long,
    val amountInCents: Long,
    val categoryId: Long,
    val type: String,
    val paymentMethod: String,
    val frequency: String,
    val startDate: Long,
    val endDate: Long? = null,
    val totalOccurrences: Int? = null,
    val generatedCount: Int = 0,
    val lastGeneratedDate: Long? = null,
    val isActive: Boolean = true,
    val isEssential: Boolean = false,
    val notes: String? = null
)

@Serializable
data class TransactionBackupDto(
    val id: Long,
    val amountInCents: Long,
    val timestamp: Long,
    val categoryId: Long,
    val type: String,
    val paymentMethod: String,
    val isEssential: Boolean = false,
    val installmentGroupId: String? = null,
    val installmentsCount: Int = 1,
    val currentInstallment: Int = 1,
    val recurringTransactionId: Long? = null,
    val notes: String? = null
)

@Serializable
data class InvestmentBackupDto(
    val id: Long,
    val name: String,
    val type: String,
    val horizon: String,
    val currentBalanceInCents: Long = 0L
)

@Serializable
data class InvestmentContributionBackupDto(
    val id: Long,
    val investmentId: Long,
    val amountInCents: Long,
    val timestamp: Long,
    val notes: String? = null
)

@Serializable
data class MonthlyBudgetBackupDto(
    val id: Long,
    val year: Int,
    val month: Int,
    val categoryId: Long,
    val plannedAmountInCents: Long
)

@Serializable
data class BackupPayloadDto(
    val version: Int = 2,
    val exportedAt: Long,
    val categories: List<CategoryBackupDto>,
    val transactions: List<TransactionBackupDto>,
    val investments: List<InvestmentBackupDto>,
    val investmentContributions: List<InvestmentContributionBackupDto>,
    val monthlyBudgets: List<MonthlyBudgetBackupDto>,
    val recurringTransactions: List<RecurringTransactionBackupDto> = emptyList()
)
