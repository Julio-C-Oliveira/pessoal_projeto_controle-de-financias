package com.example.kofre.domain.usecase.category

import com.example.kofre.domain.model.Category
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.first

interface DeleteCategoryUseCase {
    suspend operator fun invoke(category: Category): Result<Unit>
}

class DeleteCategoryUseCaseImpl(
    private val repository: FinanceRepository
) : DeleteCategoryUseCase {
    override suspend operator fun invoke(category: Category): Result<Unit> {
        return runCatching {
            val allTransactions = repository.getAllTransactions().first()
            val hasLinkedTransactions = allTransactions.any { it.categoryId == category.id }
            if (hasLinkedTransactions) {
                throw IllegalStateException("Não é possível excluir a categoria \"${category.name}\" pois existem transações vinculadas a ela.")
            }
            repository.deleteCategory(category)
        }
    }
}
