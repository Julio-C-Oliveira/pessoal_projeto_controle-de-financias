package com.example.kofre.domain.usecase.report

import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.model.Investment
import com.example.kofre.domain.model.InvestmentContribution
import com.example.kofre.domain.model.Transaction
import com.example.kofre.domain.usecase.transaction.FakeFinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GetFinancialReportUseCaseTest {

    private lateinit var repository: FakeFinanceRepository
    private lateinit var useCase: GetFinancialReportUseCaseImpl
    private val zoneId: ZoneId = ZoneId.systemDefault()

    private var expenseCatId1: Long = 0L
    private var expenseCatId2: Long = 0L
    private var incomeCatId: Long = 0L

    @Before
    fun setUp() {
        runBlocking {
            repository = FakeFinanceRepository()
            useCase = GetFinancialReportUseCaseImpl(repository, zoneId)

            expenseCatId1 = repository.insertCategory(Category(name = "Alimentação", type = CategoryType.EXPENSE))
            expenseCatId2 = repository.insertCategory(Category(name = "Lazer", type = CategoryType.EXPENSE))
            incomeCatId = repository.insertCategory(Category(name = "Salário", type = CategoryType.INCOME))
        }
    }

    private fun toMillis(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        return LocalDate.of(year, month, day)
            .atTime(hour, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    // Teste 1: PeriodType.WEEK — relatório gerado com referenceDate numa quinta-feira deve incluir apenas transações de segunda a domingo daquela semana.
    @Test
    fun testWeekPeriodFiltersCorrectlyFromMondayToSunday() = runBlocking {
        // Quinta-feira, 3 de Setembro de 2026
        val thursdayRefDate = toMillis(2026, 9, 3)

        // Domingo anterior (30 de Agosto) - Não deve incluir
        repository.insertTransaction(
            Transaction(
                amountInCents = 1000L,
                timestamp = toMillis(2026, 8, 30),
                categoryId = expenseCatId1,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.PIX
            )
        )

        // Segunda-feira (31 de Agosto) - Deve incluir
        repository.insertTransaction(
            Transaction(
                amountInCents = 2000L,
                timestamp = toMillis(2026, 8, 31, 1),
                categoryId = expenseCatId1,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.PIX
            )
        )

        // Quinta-feira (3 de Setembro) - Deve incluir
        repository.insertTransaction(
            Transaction(
                amountInCents = 3000L,
                timestamp = toMillis(2026, 9, 3, 14),
                categoryId = expenseCatId1,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.PIX
            )
        )

        // Domingo (6 de Setembro) - Deve incluir
        repository.insertTransaction(
            Transaction(
                amountInCents = 4000L,
                timestamp = toMillis(2026, 9, 6, 22),
                categoryId = expenseCatId1,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.PIX
            )
        )

        // Segunda-feira seguinte (7 de Setembro) - Não deve incluir
        repository.insertTransaction(
            Transaction(
                amountInCents = 5000L,
                timestamp = toMillis(2026, 9, 7),
                categoryId = expenseCatId1,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.PIX
            )
        )

        val report = useCase(TimeFilter(PeriodType.WEEK, thursdayRefDate)).first()

        // Soma esperada: 2000 + 3000 + 4000 = 9000
        assertEquals(9000L, report.totalExpenseInCents)
    }

    // Teste 2: PeriodType.MONTH — relatório de fevereiro não deve incluir transações de janeiro ou março.
    @Test
    fun testMonthPeriodFiltersFebruaryOnly() = runBlocking {
        val febRefDate = toMillis(2026, 2, 15)

        // 31 de Janeiro - Excluída
        repository.insertTransaction(
            Transaction(
                amountInCents = 10000L,
                timestamp = toMillis(2026, 1, 31),
                categoryId = expenseCatId1,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.PIX
            )
        )

        // 1º de Fevereiro - Incluída
        repository.insertTransaction(
            Transaction(
                amountInCents = 15000L,
                timestamp = toMillis(2026, 2, 1),
                categoryId = expenseCatId1,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.PIX
            )
        )

        // 28 de Fevereiro - Incluída
        repository.insertTransaction(
            Transaction(
                amountInCents = 25000L,
                timestamp = toMillis(2026, 2, 28, 20),
                categoryId = expenseCatId1,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.PIX
            )
        )

        // 1º de Março - Excluída
        repository.insertTransaction(
            Transaction(
                amountInCents = 30000L,
                timestamp = toMillis(2026, 3, 1),
                categoryId = expenseCatId1,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.PIX
            )
        )

        val report = useCase(TimeFilter(PeriodType.MONTH, febRefDate)).first()

        assertEquals(40000L, report.totalExpenseInCents)
    }

    // Teste 3: netBalanceInCents = totalIncomeInCents - totalExpenseInCents calculado corretamente.
    @Test
    fun testNetBalanceCalculatedCorrectly() = runBlocking {
        val refDate = toMillis(2026, 9, 15)

        // Receita: 100.000 centavos
        repository.insertTransaction(
            Transaction(
                amountInCents = 100000L,
                timestamp = refDate,
                categoryId = incomeCatId,
                type = TransactionType.INCOME,
                paymentMethod = PaymentMethod.PIX
            )
        )

        // Despesa: 40.000 centavos
        repository.insertTransaction(
            Transaction(
                amountInCents = 40000L,
                timestamp = refDate,
                categoryId = expenseCatId1,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.DEBIT
            )
        )

        val report = useCase(TimeFilter(PeriodType.MONTH, refDate)).first()

        assertEquals(100000L, report.totalIncomeInCents)
        assertEquals(40000L, report.totalExpenseInCents)
        assertEquals(60000L, report.netBalanceInCents)
    }

    // Teste 4: freeCashInCents = netBalanceInCents - totalInvestedInCents calculado corretamente.
    @Test
    fun testFreeCashCalculatedCorrectly() = runBlocking {
        val refDate = toMillis(2026, 9, 15)

        // Receita: 100.000
        repository.insertTransaction(
            Transaction(
                amountInCents = 100000L,
                timestamp = refDate,
                categoryId = incomeCatId,
                type = TransactionType.INCOME,
                paymentMethod = PaymentMethod.PIX
            )
        )

        // Despesa: 40.000 -> netBalance = 60.000
        repository.insertTransaction(
            Transaction(
                amountInCents = 40000L,
                timestamp = refDate,
                categoryId = expenseCatId1,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.DEBIT
            )
        )

        // Aporte em Investimento: 20.000
        val invId = repository.insertInvestment(
            Investment(
                name = "Tesouro Direto",
                type = InvestmentType.FIXED_INCOME,
                horizon = InvestmentHorizon.MEDIUM
            )
        )
        repository.insertContribution(
            InvestmentContribution(
                investmentId = invId,
                amountInCents = 20000L,
                timestamp = refDate
            )
        )

        val report = useCase(TimeFilter(PeriodType.MONTH, refDate)).first()

        assertEquals(60000L, report.netBalanceInCents)
        assertEquals(20000L, report.totalInvestedInCents)
        assertEquals(40000L, report.freeCashInCents)
    }

    // Teste 5: A soma de percentageOfTotal de todas as entradas em categoryExpenses deve ser igual a 100.0 (tolerância de 0.01 por arredondamento).
    @Test
    fun testCategoryExpensesPercentageSumEquals100() = runBlocking {
        val refDate = toMillis(2026, 9, 15)

        // Categoria 1: 30.000 centavos
        repository.insertTransaction(
            Transaction(
                amountInCents = 30000L,
                timestamp = refDate,
                categoryId = expenseCatId1,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.DEBIT
            )
        )

        // Categoria 2: 70.000 centavos
        repository.insertTransaction(
            Transaction(
                amountInCents = 70000L,
                timestamp = refDate,
                categoryId = expenseCatId2,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.DEBIT
            )
        )

        val report = useCase(TimeFilter(PeriodType.MONTH, refDate)).first()

        assertEquals(2, report.categoryExpenses.size)
        val sumPercentage = report.categoryExpenses.sumOf { it.percentageOfTotal }
        assertEquals(100.0, sumPercentage, 0.01)
    }

    // Teste 6: essentialExpenseInCents + nonEssentialExpenseInCents == totalExpenseInCents sempre verdadeiro.
    @Test
    fun testEssentialPlusNonEssentialEqualsTotalExpense() = runBlocking {
        val refDate = toMillis(2026, 9, 15)

        // Essencial: 35.000 centavos
        repository.insertTransaction(
            Transaction(
                amountInCents = 35000L,
                timestamp = refDate,
                categoryId = expenseCatId1,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.DEBIT,
                isEssential = true
            )
        )

        // Não Essencial: 15.000 centavos
        repository.insertTransaction(
            Transaction(
                amountInCents = 15000L,
                timestamp = refDate,
                categoryId = expenseCatId2,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.CREDIT_CARD,
                isEssential = false
            )
        )

        val report = useCase(TimeFilter(PeriodType.MONTH, refDate)).first()

        assertEquals(35000L, report.essentialExpenseInCents)
        assertEquals(15000L, report.nonEssentialExpenseInCents)
        assertEquals(50000L, report.totalExpenseInCents)
        assertEquals(report.totalExpenseInCents, report.essentialExpenseInCents + report.nonEssentialExpenseInCents)
    }
}
