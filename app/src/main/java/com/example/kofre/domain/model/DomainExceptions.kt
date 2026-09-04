package com.example.kofre.domain.model

class InvalidTransactionAmountException(
    message: String = "Valor da transação deve ser estritamente maior que zero"
) : Exception(message)

class CategoryNotFoundException(
    categoryId: Long
) : Exception("Categoria com ID $categoryId não foi encontrada")

class IncompatibleCategoryException(
    message: String = "Tipo da categoria não corresponde ao tipo da transação"
) : Exception(message)

class InvalidPaymentMethodException(
    message: String = "Parcelamento é permitido apenas para o método de pagamento Cartão de Crédito"
) : Exception(message)

class TransactionNotFoundException(
    message: String = "Transação não encontrada"
) : Exception(message)

class InvalidInvestmentNameException(
    message: String = "Nome do investimento não pode ser vazio ou conter apenas espaços em branco"
) : Exception(message)

class InvalidInvestmentBalanceException(
    message: String = "Saldo do investimento não pode ser negativo"
) : Exception(message)

class InvalidContributionAmountException(
    message: String = "Valor do aporte deve ser estritamente maior que zero"
) : Exception(message)

class InvestmentNotFoundException(
    investmentId: Long
) : Exception("Investimento com ID $investmentId não foi encontrado")
