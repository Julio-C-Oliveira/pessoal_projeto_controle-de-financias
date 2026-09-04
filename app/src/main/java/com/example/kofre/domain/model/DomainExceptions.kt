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
