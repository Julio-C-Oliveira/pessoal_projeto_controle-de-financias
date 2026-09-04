package com.example.kofre.domain.usecase.transaction

import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.model.TransactionNotFoundException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteTransactionUseCaseTest {

    private lateinit var repository: FakeFinanceRepository
    private lateinit var createUseCase: CreateTransactionUseCaseImpl
    private lateinit var deleteUseCase: DeleteTransactionUseCaseImpl

    private var categoryId: Long = 0L

    @Before
    fun setUp() {
        runBlocking {
            repository = FakeFinanceRepository()
            createUseCase = CreateTransactionUseCaseImpl(repository)
            deleteUseCase = DeleteTransactionUseCaseImpl(repository)

            categoryId = repository.insertCategory(
                Category(name = "Lazer", type = CategoryType.EXPENSE)
            )
        }
    }

    // Teste 8 (Spec 02): DeleteTransactionUseCase com deleteEntireGroup = true remove todas as parcelas do grupo.
    @Test
    fun testDeleteEntireGroupRemovesAllInstallmentsInGroup() = runBlocking {
        val params = CreateTransactionParams(
            amountInCents = 12000L,
            timestamp = System.currentTimeMillis(),
            categoryId = categoryId,
            type = TransactionType.EXPENSE,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            installmentsCount = 3
        )
        val createResult = createUseCase(params)
        assertTrue(createResult.isSuccess)
        val ids = createResult.getOrThrow()
        assertEquals(3, ids.size)

        val txListBefore = repository.getAllTransactions().first()
        assertEquals(3, txListBefore.size)

        // Deletar com deleteEntireGroup = true passando o id da 2ª parcela
        val deleteResult = deleteUseCase(ids[1], deleteEntireGroup = true)
        assertTrue(deleteResult.isSuccess)

        val txListAfter = repository.getAllTransactions().first()
        assertEquals(0, txListAfter.size)
    }

    @Test
    fun testDeleteSingleInstallmentDefaultBehavior() = runBlocking {
        val params = CreateTransactionParams(
            amountInCents = 12000L,
            timestamp = System.currentTimeMillis(),
            categoryId = categoryId,
            type = TransactionType.EXPENSE,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            installmentsCount = 3
        )
        val createResult = createUseCase(params)
        val ids = createResult.getOrThrow()

        // Deletar apenas a 1ª parcela (deleteEntireGroup = false por padrão)
        val deleteResult = deleteUseCase(ids[0], deleteEntireGroup = false)
        assertTrue(deleteResult.isSuccess)

        val txListAfter = repository.getAllTransactions().first()
        assertEquals(2, txListAfter.size)
        assertTrue(txListAfter.none { it.id == ids[0] })
    }

    @Test
    fun testDeleteNonExistentTransactionFails() = runBlocking {
        val deleteResult = deleteUseCase(999L)
        assertTrue(deleteResult.isFailure)
        assertTrue(deleteResult.exceptionOrNull() is TransactionNotFoundException)
    }
}
