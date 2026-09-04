package com.example.kofre.domain.usecase.transaction

import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.model.Transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetTransactionsUseCaseTest {

    private lateinit var repository: FakeFinanceRepository
    private lateinit var getTransactionsUseCase: GetTransactionsUseCaseImpl

    @Before
    fun setUp() {
        runBlocking {
        repository = FakeFinanceRepository()
        getTransactionsUseCase = GetTransactionsUseCaseImpl(repository)

        val catId = repository.insertCategory(
            Category(name = "Alimentação", type = CategoryType.EXPENSE)
        )

        repository.insertTransaction(
            Transaction(
                amountInCents = 1000L,
                timestamp = 100L,
                categoryId = catId,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.PIX
            )
        )
        repository.insertTransaction(
            Transaction(
                amountInCents = 2000L,
                timestamp = 200L,
                categoryId = catId,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.DEBIT
            )
        )
        repository.insertTransaction(
            Transaction(
                amountInCents = 3000L,
                timestamp = 300L,
                categoryId = catId,
                type = TransactionType.EXPENSE,
                paymentMethod = PaymentMethod.CREDIT_CARD
            )
        )
    }
}

    @Test
    fun testGetTransactionsFilterByDateRange() = runBlocking {
        val result = getTransactionsUseCase(startDate = 150L, endDate = 250L).first()

        assertEquals(1, result.size)
        assertEquals(2000L, result[0].amountInCents)
    }
}
