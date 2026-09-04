package com.example.kofre.domain.usecase.investment

import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.domain.model.InvalidContributionAmountException
import com.example.kofre.domain.model.Investment
import com.example.kofre.domain.model.InvestmentNotFoundException
import com.example.kofre.domain.usecase.transaction.FakeFinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddContributionUseCaseTest {

    private lateinit var repository: FakeFinanceRepository
    private lateinit var addContributionUseCase: AddContributionUseCase

    @Before
    fun setUp() {
        repository = FakeFinanceRepository()
        addContributionUseCase = AddContributionUseCaseImpl(repository)
    }

    // Teste 4: AddContributionUseCase deve incrementar currentBalanceInCents pelo valor exato do aporte.
    @Test
    fun testAddContributionIncrementsCurrentBalance() = runBlocking {
        val investmentId = repository.insertInvestment(
            Investment(
                name = "Tesouro IPCA+",
                type = InvestmentType.FIXED_INCOME,
                horizon = InvestmentHorizon.LONG,
                currentBalanceInCents = 50000L
            )
        )

        val params = AddContributionParams(
            investmentId = investmentId,
            amountInCents = 25000L,
            timestamp = System.currentTimeMillis(),
            notes = "Aporte mensal"
        )

        val result = addContributionUseCase(params)
        assertTrue(result.isSuccess)

        val investment = repository.getInvestmentById(investmentId).first()
        assertEquals(75000L, investment?.currentBalanceInCents)

        val contributions = repository.getContributionsByInvestmentId(investmentId).first()
        assertEquals(1, contributions.size)
        assertEquals(25000L, contributions[0].amountInCents)
        assertEquals("Aporte mensal", contributions[0].notes)
    }

    // Teste 5: Falhar se amountInCents <= 0 em AddContributionUseCase.
    @Test
    fun testAddContributionWithZeroOrNegativeAmountFails() = runBlocking {
        val investmentId = repository.insertInvestment(
            Investment(
                name = "Tesouro IPCA+",
                type = InvestmentType.FIXED_INCOME,
                horizon = InvestmentHorizon.LONG,
                currentBalanceInCents = 50000L
            )
        )

        val zeroResult = addContributionUseCase(
            AddContributionParams(investmentId = investmentId, amountInCents = 0L)
        )
        assertTrue(zeroResult.isFailure)
        assertTrue(zeroResult.exceptionOrNull() is InvalidContributionAmountException)

        val negativeResult = addContributionUseCase(
            AddContributionParams(investmentId = investmentId, amountInCents = -100L)
        )
        assertTrue(negativeResult.isFailure)
        assertTrue(negativeResult.exceptionOrNull() is InvalidContributionAmountException)
    }

    @Test
    fun testAddContributionToNonExistentInvestmentFails() = runBlocking {
        val result = addContributionUseCase(
            AddContributionParams(investmentId = 999L, amountInCents = 1000L)
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvestmentNotFoundException)
    }
}
