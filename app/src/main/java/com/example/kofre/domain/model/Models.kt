package com.example.kofre.domain.model

import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.data.local.enums.PaymentMethod
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
    val notes: String? = null
)

data class Investment(
    val id: Long = 0,
    val name: String,
    val type: InvestmentType,
    val horizon: InvestmentHorizon,
    val currentBalanceInCents: Long = 0L
)
