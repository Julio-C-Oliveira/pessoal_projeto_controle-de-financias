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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ProcessDueRecurringTransactionsUseCaseTest {

    private lateinit var repository: FakeFinanceRepository
    private lateinit var createUseCase: CreateRecurringTransactionUseCaseImpl
    private lateinit var processDueUseCase: ProcessDueRecurringTransactionsUseCaseImpl

    private var expenseCategoryId: Long = 0L
    private val zoneId = ZoneId.systemDefault()

    @Before
    fun setUp() = runBlocking {
        repository = FakeFinanceRepository()
        createUseCase = CreateRecurringTransactionUseCaseImpl(repository)
        processDueUseCase = ProcessDueRecurringTransactionsUseCaseImpl(repository)

        expenseCategoryId = repository.insertCategory(Category(name = "Alimentação", type = CategoryType.EXPENSE))
    }

    @Test
    fun `test 4 - process due recurring transactions generates all due instances and updates lastGeneratedDate`() = runBlocking {
        val now = LocalDate.of(2026, 3, 15)
        val twoMonthsAgo = LocalDate.of(2026, 1, 15)
        val startDate = twoMonthsAgo.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val currentTimestamp = now.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val createResult = createUseCase(
            CreateRecurringTransactionParams(
                amountInCents = 10000L,
                categoryId = expenseCategoryId,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.CREDIT_CARD,
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = startDate
            )
        )
        val recurringId = createResult.getOrThrow()

        val processResult = processDueUseCase(currentTimestamp)
        assertEquals(3, processResult.getOrThrow()) // Month 0 (Jan 15), Month 1 (Feb 15), Month 2 (Mar 15)

        val txList = repository.getAllTransactions().first()
        assertEquals(3, txList.size)

        val recurringList = repository.getRecurringTransactions().first()
        val rule = recurringList.first { it.id == recurringId }
        assertNotNull(rule.lastGeneratedDate)
        assertEquals(currentTimestamp, rule.lastGeneratedDate)
    }

    @Test
    fun `test 5 - inactive rules are ignored by process due use case`() = runBlocking {
        val startDate = LocalDate.of(2026, 1, 15).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val currentTimestamp = LocalDate.of(2026, 3, 15).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val createResult = createUseCase(
            CreateRecurringTransactionParams(
                amountInCents = 10000L,
                categoryId = expenseCategoryId,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.CREDIT_CARD,
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = startDate
            )
        )
        val recurringId = createResult.getOrThrow()

        val toggleUseCase = ToggleRecurringTransactionUseCaseImpl(repository)
        toggleUseCase(recurringId, false)

        val processResult = processDueUseCase(currentTimestamp)
        assertEquals(0, processResult.getOrThrow())

        val txList = repository.getAllTransactions().first()
        assertEquals(0, txList.size)
    }

    @Test
    fun `test 6 - adjust last day of month for MONTHLY frequency in short months`() = runBlocking {
        val jan31 = LocalDate.of(2026, 1, 31).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val feb28 = LocalDate.of(2026, 2, 28).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val createResult = createUseCase(
            CreateRecurringTransactionParams(
                amountInCents = 20000L,
                categoryId = expenseCategoryId,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.PIX,
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = jan31
            )
        )

        val processResult = processDueUseCase(feb28)
        assertEquals(2, processResult.getOrThrow()) // Jan 31 and Feb 28

        val txList = repository.getAllTransactions().first().sortedBy { it.timestamp }
        val febTxDate = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(txList[1].timestamp), zoneId)
        assertEquals(2026, febTxDate.year)
        assertEquals(2, febTxDate.monthValue)
        assertEquals(28, febTxDate.dayOfMonth)
    }

    @Test
    fun `test totalOccurrences limit stops generating when limit is reached`() = runBlocking {
        val startDate = LocalDate.of(2026, 1, 15).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val currentTimestamp = LocalDate.of(2026, 6, 15).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val createResult = createUseCase(
            CreateRecurringTransactionParams(
                amountInCents = 10000L,
                categoryId = expenseCategoryId,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.CREDIT_CARD,
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = startDate,
                totalOccurrences = 3 // Only 3 occurrences allowed!
            )
        )
        val recurringId = createResult.getOrThrow()

        val processResult = processDueUseCase(currentTimestamp)
        assertEquals(3, processResult.getOrThrow()) // Should generate exactly 3 occurrences (Jan, Feb, Mar)

        val txList = repository.getAllTransactions().first()
        assertEquals(3, txList.size)

        val recurringList = repository.getRecurringTransactions().first()
        val rule = recurringList.first { it.id == recurringId }
        assertEquals(3, rule.generatedCount)
    }
}
