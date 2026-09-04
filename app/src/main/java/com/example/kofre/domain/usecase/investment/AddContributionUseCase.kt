package com.example.kofre.domain.usecase.investment

import com.example.kofre.domain.model.InvalidContributionAmountException
import com.example.kofre.domain.model.InvestmentContribution
import com.example.kofre.domain.model.InvestmentNotFoundException
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.first

data class AddContributionParams(
    val investmentId: Long,
    val amountInCents: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)

interface AddContributionUseCase {
    suspend operator fun invoke(params: AddContributionParams): Result<Long>
}

class AddContributionUseCaseImpl(
    private val repository: FinanceRepository
) : AddContributionUseCase {

    override suspend operator fun invoke(params: AddContributionParams): Result<Long> {
        return runCatching {
            if (params.amountInCents <= 0) {
                throw InvalidContributionAmountException()
            }

            val investment = repository.getInvestmentById(params.investmentId).first()
                ?: throw InvestmentNotFoundException(params.investmentId)

            val contribution = InvestmentContribution(
                investmentId = params.investmentId,
                amountInCents = params.amountInCents,
                timestamp = params.timestamp,
                notes = params.notes
            )

            val contributionId = repository.insertContribution(contribution)

            val newBalance = investment.currentBalanceInCents + params.amountInCents
            repository.updateInvestmentBalance(params.investmentId, newBalance)

            contributionId
        }
    }
}
