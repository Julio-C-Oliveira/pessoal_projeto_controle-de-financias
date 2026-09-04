package com.example.kofre.domain.usecase.recurring

import com.example.kofre.data.local.entity.RecurringTransactionEntity
import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.RecurrenceFrequency
import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.first

data class CreateRecurringTransactionParams(
    val amountInCents: Long,
    val categoryId: Long,
    val type: TransactionType,
    val paymentMethod: PaymentMethod,
    val frequency: RecurrenceFrequency,
    val startDate: Long,
    val endDate: Long? = null,
    val totalOccurrences: Int? = null,
    val isEssential: Boolean = false,
    val notes: String? = null
)

interface CreateRecurringTransactionUseCase {
    suspend operator fun invoke(params: CreateRecurringTransactionParams): Result<Long>
}

class CreateRecurringTransactionUseCaseImpl(
    private val repository: FinanceRepository
) : CreateRecurringTransactionUseCase {

    override suspend operator fun invoke(params: CreateRecurringTransactionParams): Result<Long> {
        if (params.amountInCents <= 0) {
            return Result.failure(IllegalArgumentException("O valor da transação recorrente deve ser maior que zero."))
        }

        val categories = repository.getAllCategories().first()
        val category = findCategoryById(categories, params.categoryId)
            ?: return Result.failure(IllegalArgumentException("Categoria não encontrada."))

        val expectedCategoryType = if (params.type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
        if (category.type != expectedCategoryType) {
            return Result.failure(IllegalArgumentException("Categoria incompatível com o tipo da transação recorrente."))
        }

        val effectiveIsEssential = if (params.type == TransactionType.INCOME) false else params.isEssential

        val entity = RecurringTransactionEntity(
            amountInCents = params.amountInCents,
            categoryId = params.categoryId,
            type = params.type.name,
            paymentMethod = params.paymentMethod.name,
            frequency = params.frequency.name,
            startDate = params.startDate,
            endDate = params.endDate,
            totalOccurrences = params.totalOccurrences,
            generatedCount = 0,
            lastGeneratedDate = null,
            isActive = true,
            isEssential = effectiveIsEssential,
            notes = params.notes
        )

        return try {
            val id = repository.insertRecurringTransaction(entity)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findCategoryById(categories: List<com.example.kofre.domain.model.Category>, id: Long): com.example.kofre.domain.model.Category? {
        for (cat in categories) {
            if (cat.id == id) return cat
            val foundInSub = findCategoryById(cat.subcategories, id)
            if (foundInSub != null) return foundInSub
        }
        return null
    }
}
