package com.example.kofre.domain.usecase.investment

import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.domain.model.InvalidInvestmentBalanceException
import com.example.kofre.domain.model.Investment
import com.example.kofre.domain.model.InvestmentContribution
import com.example.kofre.domain.model.InvestmentNotFoundException
import com.example.kofre.domain.usecase.transaction.FakeFinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateInvestmentBalanceUseCaseTest {

    private lateinit var repository: FakeFinanceRepository
    private lateinit var useCase: UpdateInvestmentBalanceUseCase

    @Before
    fun setUp() {
        repository = FakeFinanceRepository()
        useCase = UpdateInvestmentBalanceUseCaseImpl(repository)
    }

    // Teste 6: UpdateInvestmentBalanceUseCase atualiza o saldo sem alterar o histórico de aportes existentes.
    @Test
    fun testUpdateInvestmentBalancePreservesContributionHistory() = runBlocking {
        val investmentId = repository.insertInvestment(
            Investment(
                name = "FII KNCR11",
                type = InvestmentType.VARIABLE,
                horizon = InvestmentHorizon.MEDIUM,
                currentBalanceInCents = 100000L
            )
        )

        repository.insertContribution(
            InvestmentContribution(
                investmentId = investmentId,
                amountInCents = 100000L,
                timestamp = 1600000000000L
            )
        )

        val result = useCase(investmentId, 120000L)
        assertTrue(result.isSuccess)

        val investment = repository.getInvestmentById(investmentId).first()
        assertEquals(120000L, investment?.currentBalanceInCents)

        val contributions = repository.getContributionsByInvestmentId(investmentId).first()
        assertEquals(1, contributions.size)
        assertEquals(100000L, contributions[0].amountInCents)
    }

    @Test
    fun testUpdateInvestmentBalanceWithNegativeValueFails() = runBlocking {
        val investmentId = repository.insertInvestment(
            Investment(
                name = "Ações PETR4",
                type = InvestmentType.VARIABLE,
                horizon = InvestmentHorizon.LONG,
                currentBalanceInCents = 50000L
            )
        )

        val result = useCase(investmentId, -100L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidInvestmentBalanceException)
    }

    @Test
    fun testUpdateInvestmentBalanceNonExistentFails() = runBlocking {
        val result = useCase(999L, 50000L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvestmentNotFoundException)
    }
}
