package com.example.kofre.domain.usecase.investment

import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.domain.model.InvalidInvestmentBalanceException
import com.example.kofre.domain.model.InvalidInvestmentNameException
import com.example.kofre.domain.usecase.transaction.FakeFinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateInvestmentUseCaseTest {

    private lateinit var repository: FakeFinanceRepository
    private lateinit var useCase: CreateInvestmentUseCase

    @Before
    fun setUp() {
        repository = FakeFinanceRepository()
        useCase = CreateInvestmentUseCaseImpl(repository)
    }

    // Teste 1: Falhar ao criar ativo com nome vazio ou contendo apenas espaços em branco.
    @Test
    fun testCreateInvestmentWithBlankNameFails() = runBlocking {
        val params = CreateInvestmentParams(
            name = "   ",
            type = InvestmentType.FIXED_INCOME,
            horizon = InvestmentHorizon.SHORT,
            initialAmountInCents = 1000L
        )

        val result = useCase(params)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidInvestmentNameException)
    }

    // Teste 2: Criar ativo com initialAmountInCents = 1000 deve gerar automaticamente 1 registro em InvestmentContribution com o mesmo valor e timestamp.
    @Test
    fun testCreateInvestmentWithInitialAmountGeneratesContribution() = runBlocking {
        val timestamp = 1700000000000L
        val params = CreateInvestmentParams(
            name = "CDB 100% CDI",
            type = InvestmentType.FIXED_INCOME,
            horizon = InvestmentHorizon.SHORT,
            initialAmountInCents = 1000L,
            timestamp = timestamp
        )

        val result = useCase(params)
        assertTrue(result.isSuccess)

        val investmentId = result.getOrThrow()
        assertTrue(investmentId > 0)

        val investments = repository.getAllInvestments().first()
        assertEquals(1, investments.size)
        assertEquals(1000L, investments[0].currentBalanceInCents)

        val contributions = repository.getContributionsByInvestmentId(investmentId).first()
        assertEquals(1, contributions.size)
        assertEquals(1000L, contributions[0].amountInCents)
        assertEquals(timestamp, contributions[0].timestamp)
    }

    // Teste 3: Criar ativo com initialAmountInCents = 0 não deve gerar nenhum InvestmentContribution.
    @Test
    fun testCreateInvestmentWithZeroInitialAmountDoesNotGenerateContribution() = runBlocking {
        val params = CreateInvestmentParams(
            name = "Ações Vale",
            type = InvestmentType.VARIABLE,
            horizon = InvestmentHorizon.LONG,
            initialAmountInCents = 0L
        )

        val result = useCase(params)
        assertTrue(result.isSuccess)

        val investmentId = result.getOrThrow()
        val contributions = repository.getContributionsByInvestmentId(investmentId).first()
        assertTrue(contributions.isEmpty())
    }

    @Test
    fun testCreateInvestmentWithNegativeInitialAmountFails() = runBlocking {
        val params = CreateInvestmentParams(
            name = "FII HGLG11",
            type = InvestmentType.VARIABLE,
            horizon = InvestmentHorizon.MEDIUM,
            initialAmountInCents = -500L
        )

        val result = useCase(params)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InvalidInvestmentBalanceException)
    }
}
