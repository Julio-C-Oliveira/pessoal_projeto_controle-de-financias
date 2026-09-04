package com.example.kofre.domain.usecase.transaction

import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.model.IncompatibleCategoryException
import com.example.kofre.domain.model.InvalidPaymentMethodException
import com.example.kofre.domain.model.InvalidTransactionAmountException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CreateTransactionUseCaseTest {

    private lateinit var repository: FakeFinanceRepository
    private lateinit var useCase: CreateTransactionUseCaseImpl

    private var expenseCategoryId: Long = 0L
    private var incomeCategoryId: Long = 0L

    @Before
    fun setUp() {
        runBlocking {
            repository = FakeFinanceRepository()
            useCase = CreateTransactionUseCaseImpl(repository)

            expenseCategoryId = repository.insertCategory(
                Category(name = "Alimentação", type = CategoryType.EXPENSE)
            )
            incomeCategoryId = repository.insertCategory(
                Category(name = "Salário", type = CategoryType.INCOME)
            )
        }
    }

    // Teste 1: Falhar com erro se amountInCents <= 0.
    @Test
    fun testAmountInCentsZeroOrNegativeFails() = runBlocking {
        val paramsZero = CreateTransactionParams(
            amountInCents = 0L,
            timestamp = System.currentTimeMillis(),
            categoryId = expenseCategoryId,
            type = TransactionType.EXPENSE,
            paymentMethod = PaymentMethod.PIX
        )
        val resultZero = useCase(paramsZero)
        assertTrue(resultZero.isFailure)
        assertTrue(resultZero.exceptionOrNull() is InvalidTransactionAmountException)

        val paramsNegative = paramsZero.copy(amountInCents = -5000L)
        val resultNegative = useCase(paramsNegative)
        assertTrue(resultNegative.isFailure)
        assertTrue(resultNegative.exceptionOrNull() is InvalidTransactionAmountException)
    }

    // Teste 2: Falhar se a categoria associada for de tipo diferente da transação.
    @Test
    fun testIncompatibleCategoryTypeFails() = runBlocking {
        val params = CreateTransactionParams(
            amountInCents = 5000L,
            timestamp = System.currentTimeMillis(),
            categoryId = expenseCategoryId, // Categoria do tipo EXPENSE
            type = TransactionType.INCOME,   // Transação do tipo INCOME
            paymentMethod = PaymentMethod.PIX
        )
        val result = useCase(params)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IncompatibleCategoryException)
    }

    // Teste 3: isEssential deve ser forçado para false ao criar transação INCOME.
    @Test
    fun testIncomeTransactionForcesIsEssentialToFalse() = runBlocking {
        val params = CreateTransactionParams(
            amountInCents = 100000L,
            timestamp = System.currentTimeMillis(),
            categoryId = incomeCategoryId,
            type = TransactionType.INCOME,
            paymentMethod = PaymentMethod.PIX,
            isEssential = true // Chamador passa true
        )
        val result = useCase(params)
        assertTrue(result.isSuccess)

        val transactions = repository.getAllTransactions().first()
        assertEquals(1, transactions.size)
        assertFalse(transactions[0].isEssential)
    }

    // Teste 4: Falhar se installmentsCount > 1 e paymentMethod != CREDIT_CARD.
    @Test
    fun testInstallmentsCountGreaterThanOneWithoutCreditCardFails() = runBlocking {
        val params = CreateTransactionParams(
            amountInCents = 10000L,
            timestamp = System.currentTimeMillis(),
            categoryId = expenseCategoryId,
            type = TransactionType.EXPENSE,
            paymentMethod = PaymentMethod.DEBIT, // Não é cartão de crédito
            installmentsCount = 3
        )
        val result = useCase(params)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidPaymentMethodException)
    }

    // Teste 5: Verificar algoritmo de parcelas (resto na 1ª parcela).
    @Test
    fun testInstallmentDivisionRemainderInFirstInstallment() = runBlocking {
        // Exemplo Spec: R$ 10,00 (1000 centavos) em 3x -> [334, 333, 333]
        val params10 = CreateTransactionParams(
            amountInCents = 1000L,
            timestamp = System.currentTimeMillis(),
            categoryId = expenseCategoryId,
            type = TransactionType.EXPENSE,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            installmentsCount = 3
        )
        val result10 = useCase(params10)
        assertTrue(result10.isSuccess)

        val txList10 = repository.getAllTransactions().first()
        assertEquals(3, txList10.size)
        assertEquals(334L, txList10[0].amountInCents)
        assertEquals(333L, txList10[1].amountInCents)
        assertEquals(333L, txList10[2].amountInCents)

        // Exemplo Spec: R$ 100,00 (10000 centavos) em 3x -> [3334, 3333, 3333]
        val repo2 = FakeFinanceRepository()
        val catId2 = repo2.insertCategory(Category(name = "Compras", type = CategoryType.EXPENSE))
        val useCase2 = CreateTransactionUseCaseImpl(repo2)

        val params100 = CreateTransactionParams(
            amountInCents = 10000L,
            timestamp = System.currentTimeMillis(),
            categoryId = catId2,
            type = TransactionType.EXPENSE,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            installmentsCount = 3
        )
        val result100 = useCase2(params100)
        assertTrue(result100.isSuccess)

        val txList100 = repo2.getAllTransactions().first()
        assertEquals(3, txList100.size)
        assertEquals(3334L, txList100[0].amountInCents)
        assertEquals(3333L, txList100[1].amountInCents)
        assertEquals(3333L, txList100[2].amountInCents)
    }

    // Teste 6: Verificar datas geradas - compra em 31/01 em 3x deve gerar 31/01, 28/02 e 31/03.
    @Test
    fun testInstallmentDatesAdjustmentForShortMonths() = runBlocking {
        val jan31Date = LocalDate.of(2026, 1, 31).atStartOfDay(ZoneId.systemDefault())
        val jan31Timestamp = jan31Date.toInstant().toEpochMilli()

        val params = CreateTransactionParams(
            amountInCents = 30000L,
            timestamp = jan31Timestamp,
            categoryId = expenseCategoryId,
            type = TransactionType.EXPENSE,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            installmentsCount = 3
        )
        val result = useCase(params)
        assertTrue(result.isSuccess)

        val txList = repository.getAllTransactions().first()
        assertEquals(3, txList.size)

        val date1 = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(txList[0].timestamp), ZoneId.systemDefault())
        val date2 = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(txList[1].timestamp), ZoneId.systemDefault())
        val date3 = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(txList[2].timestamp), ZoneId.systemDefault())

        assertEquals(LocalDate.of(2026, 1, 31), date1)
        assertEquals(LocalDate.of(2026, 2, 28), date2)
        assertEquals(LocalDate.of(2026, 3, 31), date3)
    }

    // Teste 7: Verificar que todas as parcelas compartilham o mesmo installmentGroupId não-nulo.
    @Test
    fun testInstallmentsShareSameNonNilInstallmentGroupId() = runBlocking {
        val params = CreateTransactionParams(
            amountInCents = 15000L,
            timestamp = System.currentTimeMillis(),
            categoryId = expenseCategoryId,
            type = TransactionType.EXPENSE,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            installmentsCount = 3
        )
        val result = useCase(params)
        assertTrue(result.isSuccess)

        val txList = repository.getAllTransactions().first()
        assertEquals(3, txList.size)

        val groupId = txList[0].installmentGroupId
        assertNotNull(groupId)
        assertTrue(groupId!!.isNotBlank())

        assertTrue(txList.all { it.installmentGroupId == groupId })
        assertEquals(1, txList[0].currentInstallment)
        assertEquals(2, txList[1].currentInstallment)
        assertEquals(3, txList[2].currentInstallment)

        val singleParams = CreateTransactionParams(
            amountInCents = 5000L,
            timestamp = System.currentTimeMillis(),
            categoryId = expenseCategoryId,
            type = TransactionType.EXPENSE,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            installmentsCount = 1
        )
        useCase(singleParams)
        val allTx = repository.getAllTransactions().first()
        val singleTx = allTx.last()
        assertNull(singleTx.installmentGroupId)
    }
}
