package com.example.kofre.domain.usecase.transaction

import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.model.CategoryNotFoundException
import com.example.kofre.domain.model.IncompatibleCategoryException
import com.example.kofre.domain.model.InvalidPaymentMethodException
import com.example.kofre.domain.model.InvalidTransactionAmountException
import com.example.kofre.domain.model.Transaction
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class CreateTransactionParams(
    val amountInCents: Long,
    val timestamp: Long,
    val categoryId: Long,
    val type: TransactionType,
    val paymentMethod: PaymentMethod,
    val isEssential: Boolean = false,
    val installmentsCount: Int = 1,
    val notes: String? = null
)

interface CreateTransactionUseCase {
    suspend operator fun invoke(params: CreateTransactionParams): Result<List<Long>>
}

class CreateTransactionUseCaseImpl(
    private val repository: FinanceRepository
) : CreateTransactionUseCase {

    override suspend operator fun invoke(params: CreateTransactionParams): Result<List<Long>> {
        return runCatching {
            if (params.amountInCents <= 0) {
                throw InvalidTransactionAmountException()
            }

            if (params.installmentsCount > 1 && params.paymentMethod != PaymentMethod.CREDIT_CARD) {
                throw InvalidPaymentMethodException()
            }

            val categories = repository.getAllCategories().first()
            val category = findCategoryById(categories, params.categoryId)
                ?: throw CategoryNotFoundException(params.categoryId)

            if (category.type.name != params.type.name) {
                throw IncompatibleCategoryException()
            }

            val effectiveIsEssential = if (params.type == TransactionType.INCOME) {
                false
            } else {
                params.isEssential
            }

            if (params.installmentsCount <= 1) {
                val transaction = Transaction(
                    amountInCents = params.amountInCents,
                    timestamp = params.timestamp,
                    categoryId = params.categoryId,
                    type = params.type,
                    paymentMethod = params.paymentMethod,
                    isEssential = effectiveIsEssential,
                    installmentGroupId = null,
                    installmentsCount = 1,
                    currentInstallment = 1,
                    notes = params.notes
                )
                val id = repository.insertTransaction(transaction)
                listOf(id)
            } else {
                val groupId = UUID.randomUUID().toString()
                val n = params.installmentsCount
                val baseAmount = params.amountInCents / n
                val remainder = params.amountInCents % n

                val baseZdt = Instant.ofEpochMilli(params.timestamp).atZone(ZoneId.systemDefault())

                val transactions = (1..n).map { index ->
                    val amount = if (index == 1) baseAmount + remainder else baseAmount
                    val installmentZdt = baseZdt.plusMonths((index - 1).toLong())
                    val installmentTimestamp = installmentZdt.toInstant().toEpochMilli()

                    Transaction(
                        amountInCents = amount,
                        timestamp = installmentTimestamp,
                        categoryId = params.categoryId,
                        type = params.type,
                        paymentMethod = params.paymentMethod,
                        isEssential = effectiveIsEssential,
                        installmentGroupId = groupId,
                        installmentsCount = n,
                        currentInstallment = index,
                        notes = params.notes
                    )
                }

                repository.insertTransactions(transactions)
            }
        }
    }

    private fun findCategoryById(categories: List<Category>, categoryId: Long): Category? {
        for (cat in categories) {
            if (cat.id == categoryId) return cat
            val sub = findCategoryById(cat.subcategories, categoryId)
            if (sub != null) return sub
        }
        return null
    }
}
