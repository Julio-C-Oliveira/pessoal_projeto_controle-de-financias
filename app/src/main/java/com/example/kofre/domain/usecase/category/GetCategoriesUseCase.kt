package com.example.kofre.domain.usecase.category

import com.example.kofre.domain.model.Category
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow

interface GetCategoriesUseCase {
    operator fun invoke(): Flow<List<Category>>
}

class GetCategoriesUseCaseImpl(
    private val repository: FinanceRepository
) : GetCategoriesUseCase {
    override operator fun invoke(): Flow<List<Category>> {
        return repository.getAllCategories()
    }
}
