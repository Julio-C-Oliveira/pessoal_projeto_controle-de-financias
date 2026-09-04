package com.example.kofre.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.usecase.category.AddCategoryUseCase
import com.example.kofre.domain.usecase.category.DeleteCategoryUseCase
import com.example.kofre.domain.usecase.category.GetCategoriesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CategoriesUiState(
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CategoriesViewModel(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CategoriesUiState> = combine(
        getCategoriesUseCase(),
        _errorMessage
    ) { categories, error ->
        CategoriesUiState(
            expenseCategories = categories.filter { it.type == CategoryType.EXPENSE },
            incomeCategories = categories.filter { it.type == CategoryType.INCOME },
            isLoading = false,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState(isLoading = true)
    )

    fun clearError() {
        _errorMessage.value = null
    }

    suspend fun addCategory(name: String, type: CategoryType): Boolean {
        _errorMessage.value = null
        val result = addCategoryUseCase(name, type)
        return if (result.isSuccess) {
            true
        } else {
            _errorMessage.value = result.exceptionOrNull()?.message ?: "Erro ao adicionar categoria."
            false
        }
    }

    suspend fun deleteCategory(category: Category): Boolean {
        _errorMessage.value = null
        val result = deleteCategoryUseCase(category)
        return if (result.isSuccess) {
            true
        } else {
            _errorMessage.value = result.exceptionOrNull()?.message ?: "Erro ao remover categoria."
            false
        }
    }
}
