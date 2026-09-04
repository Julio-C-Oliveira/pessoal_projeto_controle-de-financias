package com.example.kofre.domain.usecase.budget

import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.model.IncompatibleCategoryException
import com.example.kofre.domain.model.InvalidBudgetAmountException
import com.example.kofre.domain.model.MonthlyBudget
import com.example.kofre.domain.model.Transaction
import com.example.kofre.domain.usecase.transaction.FakeFinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class MonthlyBudgetTest {

    private lateinit var repository: FakeFinanceRepository
    private lateinit var setCategoryBudgetUseCase: SetCategoryBudgetUseCase
    private lateinit var removeCategoryBudgetUseCase: RemoveCategoryBudgetUseCase
    private lateinit var getMonthlyBudgetOverviewUseCase: GetMonthlyBudgetOverviewUseCase
    private lateinit var copyBudgetUseCase: CopyBudgetUseCase

    private var expenseCategoryId1: Long = 0L
    private var expenseCategoryId2: Long = 0L
    private var incomeCategoryId: Long = 0L

    @Before
    fun setUp() = runBlocking {
        repository = FakeFinanceRepository()
        setCategoryBudgetUseCase = SetCategoryBudgetUseCaseImpl(repository)
        removeCategoryBudgetUseCase = RemoveCategoryBudgetUseCaseImpl(repository)
        getMonthlyBudgetOverviewUseCase = GetMonthlyBudgetOverviewUseCaseImpl(repository)
        copyBudgetUseCase = CopyBudgetUseCaseImpl(repository)

        expenseCategoryId1 = repository.insertCategory(
            Category(name = "Alimentação", type = CategoryType.EXPENSE)
        )
        expenseCategoryId2 = repository.insertCategory(
            Category(name = "Lazer", type = CategoryType.EXPENSE)
        )
        incomeCategoryId = repository.insertCategory(
            Category(name = "Salário", type = CategoryType.INCOME)
        )
    }

    // Teste 1: Falhar ao criar orçamento para categoria do tipo INCOME ou INVESTMENT.
    @Test
    fun test1_failCreateBudgetForNonExpenseCategory() = runBlocking {
        val paramsIncome = SetBudgetParams(
            year = 2026,
            month = 9,
            categoryId = incomeCategoryId,
            plannedAmountInCents = 100000L
        )

        val resultIncome = setCategoryBudgetUseCase(paramsIncome)
        assertTrue(resultIncome.isFailure)
        assertTrue(resultIncome.exceptionOrNull() is IncompatibleCategoryException)
    }

    // Teste 2: Falhar se plannedAmountInCents <= 0.
    @Test
    fun test2_failCreateBudgetWithZeroOrNegativeAmount() = runBlocking {
        val paramsZero = SetBudgetParams(
            year = 2026,
            month = 9,
            categoryId = expenseCategoryId1,
            plannedAmountInCents = 0L
        )
        val resultZero = setCategoryBudgetUseCase(paramsZero)
        assertTrue(resultZero.isFailure)
        assertTrue(resultZero.exceptionOrNull() is InvalidBudgetAmountException)

        val paramsNegative = SetBudgetParams(
            year = 2026,
            month = 9,
            categoryId = expenseCategoryId1,
            plannedAmountInCents = -5000L
        )
        val resultNegative = setCategoryBudgetUseCase(paramsNegative)
        assertTrue(resultNegative.isFailure)
        assertTrue(resultNegative.exceptionOrNull() is InvalidBudgetAmountException)
    }

    // Teste 3: Inserir dois orçamentos para a mesma (year, month, categoryId) deve resultar em upsert (atualização), não duplicata.
    @Test
    fun test3_upsertBudgetOnDuplicateYearMonthCategory() = runBlocking {
        val params1 = SetBudgetParams(
            year = 2026,
            month = 9,
            categoryId = expenseCategoryId1,
            plannedAmountInCents = 50000L
        )
        setCategoryBudgetUseCase(params1)

        val params2 = SetBudgetParams(
            year = 2026,
            month = 9,
            categoryId = expenseCategoryId1,
            plannedAmountInCents = 80000L
        )
        setCategoryBudgetUseCase(params2)

        val budgets = repository.getBudgetsForMonth(2026, 9).first()
        assertEquals(1, budgets.size)
        assertEquals(80000L, budgets[0].plannedAmountInCents)
    }

    // Teste 4: isExceeded = true quando actualSpentInCents > plannedAmountInCents.
    @Test
    fun test4_isExceededTrueWhenSpentExceedsPlanned() = runBlocking {
        setCategoryBudgetUseCase(
            SetBudgetParams(
                year = 2026,
                month = 9,
                categoryId = expenseCategoryId1,
                plannedAmountInCents = 10000L // R$ 100,00
            )
        )

        // Timestamp em meados de setembro/2026
        val timestamp = getTimestampFor(2026, 9, 15)

        repository.insertTransaction(
            Transaction(
                amountInCents = 15000L, // R$ 150,00 (gasto > planejado)
                timestamp = timestamp,
                categoryId = expenseCategoryId1,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.DEBIT
            )
        )

        val overview = getMonthlyBudgetOverviewUseCase(2026, 9).first()
        assertEquals(1, overview.categoriesComparison.size)
        val comp = overview.categoriesComparison[0]

        assertTrue(comp.isExceeded)
        assertEquals(15000L, comp.actualSpentInCents)
        assertEquals(10000L, comp.plannedAmountInCents)
    }

    // Teste 5: remainingInCents deve ser negativo quando o orçamento é estourado.
    @Test
    fun test5_remainingInCentsNegativeWhenBudgetExceeded() = runBlocking {
        setCategoryBudgetUseCase(
            SetBudgetParams(
                year = 2026,
                month = 9,
                categoryId = expenseCategoryId1,
                plannedAmountInCents = 20000L // R$ 200,00
            )
        )

        val timestamp = getTimestampFor(2026, 9, 10)

        repository.insertTransaction(
            Transaction(
                amountInCents = 25000L, // R$ 250,00
                timestamp = timestamp,
                categoryId = expenseCategoryId1,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.CREDIT_CARD
            )
        )

        val overview = getMonthlyBudgetOverviewUseCase(2026, 9).first()
        val comp = overview.categoriesComparison[0]

        assertEquals(-5000L, comp.remainingInCents)
        assertTrue(comp.remainingInCents < 0)
    }

    // Teste 6: CopyBudgetUseCase com overrideExisting = false não deve sobrescrever metas já existentes no mês destino; deve ignorar silenciosamente as duplicatas e retornar apenas a contagem de itens novos inseridos.
    @Test
    fun test6_copyBudgetOverrideExistingFalse() = runBlocking {
        // Mês origem: Setembro/2026 com 2 orçamentos
        setCategoryBudgetUseCase(SetBudgetParams(2026, 9, expenseCategoryId1, 50000L))
        setCategoryBudgetUseCase(SetBudgetParams(2026, 9, expenseCategoryId2, 30000L))

        // Mês destino: Outubro/2026 já possui orçamento para expenseCategoryId1
        setCategoryBudgetUseCase(SetBudgetParams(2026, 10, expenseCategoryId1, 40000L))

        val result = copyBudgetUseCase(
            fromYear = 2026,
            fromMonth = 9,
            toYear = 2026,
            toMonth = 10,
            overrideExisting = false
        )

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()) // Apenas expenseCategoryId2 foi clonado

        val destBudgets = repository.getBudgetsForMonth(2026, 10).first()
        assertEquals(2, destBudgets.size)

        val cat1Budget = destBudgets.find { it.categoryId == expenseCategoryId1 }
        assertEquals(40000L, cat1Budget?.plannedAmountInCents) // Mantido o valor original (40000)

        val cat2Budget = destBudgets.find { it.categoryId == expenseCategoryId2 }
        assertEquals(30000L, cat2Budget?.plannedAmountInCents) // Clonado da origem
    }

    // Teste 7: CopyBudgetUseCase com overrideExisting = true deve substituir metas existentes e retornar a contagem total copiada.
    @Test
    fun test7_copyBudgetOverrideExistingTrue() = runBlocking {
        setCategoryBudgetUseCase(SetBudgetParams(2026, 9, expenseCategoryId1, 50000L))
        setCategoryBudgetUseCase(SetBudgetParams(2026, 9, expenseCategoryId2, 30000L))

        setCategoryBudgetUseCase(SetBudgetParams(2026, 10, expenseCategoryId1, 40000L))

        val result = copyBudgetUseCase(
            fromYear = 2026,
            fromMonth = 9,
            toYear = 2026,
            toMonth = 10,
            overrideExisting = true
        )

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()) // Ambos foram copiados/sobrescritos

        val destBudgets = repository.getBudgetsForMonth(2026, 10).first()
        assertEquals(2, destBudgets.size)

        val cat1Budget = destBudgets.find { it.categoryId == expenseCategoryId1 }
        assertEquals(50000L, cat1Budget?.plannedAmountInCents) // Sobrescrito com o valor da origem (50000)
    }

    // Teste 8: Copiar de um mês vazio (sem metas) deve retornar Result.success(0) sem erros.
    @Test
    fun test8_copyFromEmptyMonthReturnsSuccessZero() = runBlocking {
        val result = copyBudgetUseCase(
            fromYear = 2026,
            fromMonth = 1,
            toYear = 2026,
            toMonth = 2,
            overrideExisting = false
        )

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    private fun getTimestampFor(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        return cal.timeInMillis
    }
}
