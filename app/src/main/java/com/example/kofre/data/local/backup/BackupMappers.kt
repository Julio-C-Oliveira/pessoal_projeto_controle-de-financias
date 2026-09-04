package com.example.kofre.data.local.backup

import com.example.kofre.data.local.entity.CategoryEntity
import com.example.kofre.data.local.entity.InvestmentContributionEntity
import com.example.kofre.data.local.entity.InvestmentEntity
import com.example.kofre.data.local.entity.MonthlyBudgetEntity
import com.example.kofre.data.local.entity.TransactionEntity

fun CategoryEntity.toBackupDto(): CategoryBackupDto = CategoryBackupDto(
    id = id,
    name = name,
    type = type,
    parentId = parentId
)

fun CategoryBackupDto.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    type = type,
    parentId = parentId
)

fun TransactionEntity.toBackupDto(): TransactionBackupDto = TransactionBackupDto(
    id = id,
    amountInCents = amountInCents,
    timestamp = timestamp,
    categoryId = categoryId,
    type = type,
    paymentMethod = paymentMethod,
    isEssential = isEssential,
    installmentGroupId = installmentGroupId,
    installmentsCount = installmentsCount,
    currentInstallment = currentInstallment,
    notes = notes
)

fun TransactionBackupDto.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amountInCents = amountInCents,
    timestamp = timestamp,
    categoryId = categoryId,
    type = type,
    paymentMethod = paymentMethod,
    isEssential = isEssential,
    installmentGroupId = installmentGroupId,
    installmentsCount = installmentsCount,
    currentInstallment = currentInstallment,
    notes = notes
)

fun InvestmentEntity.toBackupDto(): InvestmentBackupDto = InvestmentBackupDto(
    id = id,
    name = name,
    type = type,
    horizon = horizon,
    currentBalanceInCents = currentBalanceInCents
)

fun InvestmentBackupDto.toEntity(): InvestmentEntity = InvestmentEntity(
    id = id,
    name = name,
    type = type,
    horizon = horizon,
    currentBalanceInCents = currentBalanceInCents
)

fun InvestmentContributionEntity.toBackupDto(): InvestmentContributionBackupDto = InvestmentContributionBackupDto(
    id = id,
    investmentId = investmentId,
    amountInCents = amountInCents,
    timestamp = timestamp,
    notes = notes
)

fun InvestmentContributionBackupDto.toEntity(): InvestmentContributionEntity = InvestmentContributionEntity(
    id = id,
    investmentId = investmentId,
    amountInCents = amountInCents,
    timestamp = timestamp,
    notes = notes
)

fun MonthlyBudgetEntity.toBackupDto(): MonthlyBudgetBackupDto = MonthlyBudgetBackupDto(
    id = id,
    year = year,
    month = month,
    categoryId = categoryId,
    plannedAmountInCents = plannedAmountInCents
)

fun MonthlyBudgetBackupDto.toEntity(): MonthlyBudgetEntity = MonthlyBudgetEntity(
    id = id,
    year = year,
    month = month,
    categoryId = categoryId,
    plannedAmountInCents = plannedAmountInCents
)
