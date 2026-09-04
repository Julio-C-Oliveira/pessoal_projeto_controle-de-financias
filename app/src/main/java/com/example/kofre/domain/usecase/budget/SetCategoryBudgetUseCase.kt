package com.example.kofre.domain.usecase.budget

import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.model.CategoryNotFoundException
import com.example.kofre.domain.model.IncompatibleCategoryException
import com.example.kofre.domain.model.InvalidBudgetAmountException
import com.example.kofre.domain.model.MonthlyBudget
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.first

data class SetBudgetParams(
    val year: Int,
    val month: Int,
    val categoryId: Long,
    val plannedAmountInCents: Long
)

interface SetCategoryBudgetUseCase {
    suspend operator fun invoke(params: SetBudgetParams): Result<Long>
}

class SetCategoryBudgetUseCaseImpl(
    private val repository: FinanceRepository
) : SetCategoryBudgetUseCase {

    override suspend operator fun invoke(params: SetBudgetParams): Result<Long> {
        return runCatching {
            if (params.plannedAmountInCents <= 0) {
                throw InvalidBudgetAmountException()
            }

            val categories = repository.getAllCategories().first()
            val category = findCategoryById(categories, params.categoryId)
                ?: throw CategoryNotFoundException(params.categoryId)

            if (category.type != CategoryType.EXPENSE) {
                throw IncompatibleCategoryException("Apenas categorias do tipo EXPENSE podem receber orçamento")
            }

            val budget = MonthlyBudget(
                year = params.year,
                month = params.month,
                categoryId = params.categoryId,
                plannedAmountInCents = params.plannedAmountInCents
            )

            repository.insertBudget(budget)
        }
    }

    private fun findCategoryById(categories: List<Category>, id: Long): Category? {
        for (cat in categories) {
            if (cat.id == id) return cat
            val foundInSub = findCategoryById(cat.subcategories, id)
            if (foundInSub != null) return foundInSub
        }
        return null
    }
}
