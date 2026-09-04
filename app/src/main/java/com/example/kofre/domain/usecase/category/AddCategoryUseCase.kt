package com.example.kofre.domain.usecase.category

import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.repository.FinanceRepository

interface AddCategoryUseCase {
    suspend operator fun invoke(name: String, type: CategoryType): Result<Long>
}

class AddCategoryUseCaseImpl(
    private val repository: FinanceRepository
) : AddCategoryUseCase {
    override suspend operator fun invoke(name: String, type: CategoryType): Result<Long> {
        return runCatching {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                throw IllegalArgumentException("O nome da categoria não pode ser vazio.")
            }
            val category = Category(
                name = trimmedName,
                type = type
            )
            repository.insertCategory(category)
        }
    }
}
