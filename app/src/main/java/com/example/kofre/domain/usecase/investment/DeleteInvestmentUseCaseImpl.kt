package com.example.kofre.domain.usecase.investment

import com.example.kofre.domain.repository.FinanceRepository

class DeleteInvestmentUseCaseImpl(
    private val repository: FinanceRepository
) : DeleteInvestmentUseCase {
    override suspend operator fun invoke(investmentId: Long): Result<Unit> {
        return runCatching { repository.deleteInvestment(investmentId) }
    }
}
