package com.example.kofre.domain.usecase.investment

import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.domain.model.InvalidInvestmentBalanceException
import com.example.kofre.domain.model.InvalidInvestmentNameException
import com.example.kofre.domain.model.Investment
import com.example.kofre.domain.model.InvestmentContribution
import com.example.kofre.domain.repository.FinanceRepository

data class CreateInvestmentParams(
    val name: String,
    val type: InvestmentType,
    val horizon: InvestmentHorizon,
    val initialAmountInCents: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

interface CreateInvestmentUseCase {
    suspend operator fun invoke(params: CreateInvestmentParams): Result<Long>
}

class CreateInvestmentUseCaseImpl(
    private val repository: FinanceRepository
) : CreateInvestmentUseCase {

    override suspend operator fun invoke(params: CreateInvestmentParams): Result<Long> {
        return runCatching {
            if (params.name.isBlank()) {
                throw InvalidInvestmentNameException()
            }
            if (params.initialAmountInCents < 0) {
                throw InvalidInvestmentBalanceException()
            }

            val investment = Investment(
                name = params.name,
                type = params.type,
                horizon = params.horizon,
                currentBalanceInCents = params.initialAmountInCents
            )

            val investmentId = repository.insertInvestment(investment)

            if (params.initialAmountInCents > 0) {
                val contribution = InvestmentContribution(
                    investmentId = investmentId,
                    amountInCents = params.initialAmountInCents,
                    timestamp = params.timestamp
                )
                repository.insertContribution(contribution)
            }

            investmentId
        }
    }
}
