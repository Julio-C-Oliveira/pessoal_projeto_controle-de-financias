package com.example.kofre.domain.usecase.investment

import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.domain.model.Investment
import com.example.kofre.domain.model.InvestmentContribution
import com.example.kofre.domain.usecase.transaction.FakeFinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetInvestmentsSummaryUseCaseTest {

    private lateinit var repository: FakeFinanceRepository
    private lateinit var useCase: GetInvestmentsSummaryUseCase

    @Before
    fun setUp() {
        repository = FakeFinanceRepository()
        useCase = GetInvestmentsSummaryUseCaseImpl(repository)
    }

    // Teste 7: GetInvestmentsSummaryUseCase calcula totalYieldInCents = totalInvested - totalContributions corretamente.
    @Test
    fun testGetInvestmentsSummaryCalculatesYieldCorrectly() = runBlocking {
        // Inv 1: Saldo 15000, Aporte 10000 -> Rendimento +5000
        val inv1Id = repository.insertInvestment(
            Investment(
                name = "Tesouro Selic",
                type = InvestmentType.FIXED_INCOME,
                horizon = InvestmentHorizon.SHORT,
                currentBalanceInCents = 15000L
            )
        )
        repository.insertContribution(
            InvestmentContribution(
                investmentId = inv1Id,
                amountInCents = 10000L,
                timestamp = System.currentTimeMillis()
            )
        )

        // Inv 2: Saldo 18000, Aporte 20000 -> Rendimento -2000
        val inv2Id = repository.insertInvestment(
            Investment(
                name = "Fundo Imobiliário",
                type = InvestmentType.VARIABLE,
                horizon = InvestmentHorizon.MEDIUM,
                currentBalanceInCents = 18000L
            )
        )
        repository.insertContribution(
            InvestmentContribution(
                investmentId = inv2Id,
                amountInCents = 20000L,
                timestamp = System.currentTimeMillis()
            )
        )

        val summary = useCase().first()

        assertEquals(33000L, summary.totalInvestedInCents)
        assertEquals(30000L, summary.totalContributionsInCents)
        assertEquals(3000L, summary.totalYieldInCents)
        assertEquals(2, summary.investments.size)

        val inv1Detail = summary.investments.find { it.id == inv1Id }
        assertEquals(15000L, inv1Detail?.currentBalanceInCents)
        assertEquals(10000L, inv1Detail?.totalAportadoInCents)

        val inv2Detail = summary.investments.find { it.id == inv2Id }
        assertEquals(18000L, inv2Detail?.currentBalanceInCents)
        assertEquals(20000L, inv2Detail?.totalAportadoInCents)
    }
}
