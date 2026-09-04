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

class RecurringTransactionManagementTest {

    private lateinit var repository: FakeFinanceRepository
    private lateinit var createUseCase: CreateRecurringTransactionUseCaseImpl
    private lateinit var processDueUseCase: ProcessDueRecurringTransactionsUseCaseImpl
    private lateinit var toggleUseCase: ToggleRecurringTransactionUseCaseImpl
    private lateinit var deleteUseCase: DeleteRecurringTransactionUseCaseImpl

    private var expenseCategoryId: Long = 0L

    @Before
    fun setUp() = runBlocking {
        repository = FakeFinanceRepository()
        createUseCase = CreateRecurringTransactionUseCaseImpl(repository)
        processDueUseCase = ProcessDueRecurringTransactionsUseCaseImpl(repository)
        toggleUseCase = ToggleRecurringTransactionUseCaseImpl(repository)
        deleteUseCase = DeleteRecurringTransactionUseCaseImpl(repository)

        expenseCategoryId = repository.insertCategory(Category(name = "Alimentação", type = CategoryType.EXPENSE))
    }

    @Test
    fun `test 7 - ToggleRecurringTransactionUseCase changes isActive status`() = runBlocking {
        val createResult = createUseCase(
            CreateRecurringTransactionParams(
                amountInCents = 15000L,
                categoryId = expenseCategoryId,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.DEBIT,
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = System.currentTimeMillis()
            )
        )
        val recurringId = createResult.getOrThrow()

        val ruleBefore = repository.getRecurringTransactions().first().first { it.id == recurringId }
        assertTrue(ruleBefore.isActive)

        val toggleOffResult = toggleUseCase(recurringId, false)
        assertTrue(toggleOffResult.isSuccess)

        val ruleAfterOff = repository.getRecurringTransactions().first().first { it.id == recurringId }
        assertFalse(ruleAfterOff.isActive)

        val toggleOnResult = toggleUseCase(recurringId, true)
        assertTrue(toggleOnResult.isSuccess)

        val ruleAfterOn = repository.getRecurringTransactions().first().first { it.id == recurringId }
        assertTrue(ruleAfterOn.isActive)
    }

    @Test
    fun `test 8 - DeleteRecurringTransactionUseCase deletes rule without deleting generated transactions`() = runBlocking {
        val now = System.currentTimeMillis()
        val createResult = createUseCase(
            CreateRecurringTransactionParams(
                amountInCents = 25000L,
                categoryId = expenseCategoryId,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.PIX,
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = now
            )
        )
        val recurringId = createResult.getOrThrow()

        processDueUseCase(now)

        val transactionsBefore = repository.getAllTransactions().first()
        assertEquals(1, transactionsBefore.size)

        val deleteResult = deleteUseCase(recurringId)
        assertTrue(deleteResult.isSuccess)

        val recurringListAfter = repository.getRecurringTransactions().first()
        assertTrue(recurringListAfter.none { it.id == recurringId })

        val transactionsAfter = repository.getAllTransactions().first()
        assertEquals("Generated transactions must remain intact after deleting recurring rule", 1, transactionsAfter.size)
    }
}
