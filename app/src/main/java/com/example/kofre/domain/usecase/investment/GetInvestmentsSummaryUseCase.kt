package com.example.kofre.domain.usecase.investment

import com.example.kofre.domain.model.Investment
import com.example.kofre.domain.model.InvestmentContribution
import com.example.kofre.domain.model.InvestmentDetail
import com.example.kofre.domain.model.InvestmentSummary
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface GetInvestmentsSummaryUseCase {
    operator fun invoke(): Flow<InvestmentSummary>
}

class GetInvestmentsSummaryUseCaseImpl(
    private val repository: FinanceRepository
) : GetInvestmentsSummaryUseCase {

    override operator fun invoke(): Flow<InvestmentSummary> {
        return combine(
            repository.getAllInvestments(),
            repository.getAllContributions()
        ) { investments: List<Investment>, contributions: List<InvestmentContribution> ->
            val contribMap = contributions.groupBy { it.investmentId }

            val details = investments.map { inv ->
                val totalAportado = contribMap[inv.id]?.sumOf { it.amountInCents } ?: 0L
                InvestmentDetail(
                    id = inv.id,
                    name = inv.name,
                    type = inv.type,
                    horizon = inv.horizon,
                    currentBalanceInCents = inv.currentBalanceInCents,
                    totalAportadoInCents = totalAportado
                )
            }

            val totalInvested = details.sumOf { it.currentBalanceInCents }
            val totalContributions = details.sumOf { it.totalAportadoInCents }
            val totalYield = totalInvested - totalContributions

            InvestmentSummary(
                totalInvestedInCents = totalInvested,
                totalContributionsInCents = totalContributions,
                totalYieldInCents = totalYield,
                investments = details
            )
        }
    }
}
