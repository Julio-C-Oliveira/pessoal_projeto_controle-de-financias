package com.example.kofre.domain.usecase.investment

import com.example.kofre.domain.model.InvalidInvestmentBalanceException
import com.example.kofre.domain.model.InvestmentNotFoundException
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.first

interface UpdateInvestmentBalanceUseCase {
    suspend operator fun invoke(investmentId: Long, newBalanceInCents: Long): Result<Unit>
}

class UpdateInvestmentBalanceUseCaseImpl(
    private val repository: FinanceRepository
) : UpdateInvestmentBalanceUseCase {

    override suspend operator fun invoke(investmentId: Long, newBalanceInCents: Long): Result<Unit> {
        return runCatching {
            if (newBalanceInCents < 0) {
                throw InvalidInvestmentBalanceException()
            }

            val investment = repository.getInvestmentById(investmentId).first()
                ?: throw InvestmentNotFoundException(investmentId)

            repository.updateInvestmentBalance(investment.id, newBalanceInCents)
        }
    }
}
