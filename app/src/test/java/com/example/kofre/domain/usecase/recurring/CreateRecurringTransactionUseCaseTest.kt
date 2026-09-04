package com.example.kofre.domain.usecase.recurring

import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.RecurrenceFrequency
import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.usecase.transaction.FakeFinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateRecurringTransactionUseCaseTest {

    private lateinit var repository: FakeFinanceRepository
    private lateinit var createRecurringUseCase: CreateRecurringTransactionUseCaseImpl

    private var expenseCategoryId: Long = 0L
    private var incomeCategoryId: Long = 0L

    @Before
    fun setUp() = runBlocking {
        repository = FakeFinanceRepository()
        createRecurringUseCase = CreateRecurringTransactionUseCaseImpl(repository)

        expenseCategoryId = repository.insertCategory(Category(name = "Alimentação", type = CategoryType.EXPENSE))
        incomeCategoryId = repository.insertCategory(Category(name = "Salário", type = CategoryType.INCOME))
    }

    @Test
    fun `test 1 - fail when amountInCents is less than or equal to zero`() = runBlocking {
        val paramsZero = CreateRecurringTransactionParams(
            amountInCents = 0L,
            categoryId = expenseCategoryId,
            type = TransactionType.EXPENSE,
            paymentMethod = PaymentMethod.PIX,
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = System.currentTimeMillis()
        )
        val resultZero = createRecurringUseCase(paramsZero)
        assertTrue(resultZero.isFailure)

        val paramsNegative = paramsZero.copy(amountInCents = -1000L)
        val resultNegative = createRecurringUseCase(paramsNegative)
        assertTrue(resultNegative.isFailure)
    }

    @Test
    fun `test 2 - fail when category type is incompatible with recurring transaction type`() = runBlocking {
        val paramsMismatched = CreateRecurringTransactionParams(
            amountInCents = 5000L,
            categoryId = expenseCategoryId, // Expense category with INCOME transaction
            type = TransactionType.INCOME,
            paymentMethod = PaymentMethod.PIX,
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = System.currentTimeMillis()
        )
        val result = createRecurringUseCase(paramsMismatched)
        assertTrue(result.isFailure)
    }

    @Test
    fun `test 3 - force isEssential to false for INCOME recurring transactions`() = runBlocking {
        val paramsIncomeEssential = CreateRecurringTransactionParams(
            amountInCents = 150000L,
            categoryId = incomeCategoryId,
            type = TransactionType.INCOME,
            paymentMethod = PaymentMethod.PIX,
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = System.currentTimeMillis(),
            isEssential = true // Caller sends true
        )
        val result = createRecurringUseCase(paramsIncomeEssential)
        assertTrue(result.isSuccess)

        val recurringList = repository.getRecurringTransactions().first()
        assertEquals(1, recurringList.size)
        assertFalse("isEssential must be forced to false for INCOME", recurringList.first().isEssential)
    }
}
