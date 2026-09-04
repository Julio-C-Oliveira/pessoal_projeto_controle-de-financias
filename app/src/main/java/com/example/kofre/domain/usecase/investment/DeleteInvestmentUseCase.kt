package com.example.kofre.domain.usecase.investment

interface DeleteInvestmentUseCase {
    suspend operator fun invoke(investmentId: Long): Result<Unit>
}
