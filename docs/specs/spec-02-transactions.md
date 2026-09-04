# Spec 02: Regras de Domínio, Transações e Parcelamentos

## 1. Visão Geral
Esta especificação define as regras de negócio para manipulação de transações financeiras (receitas e despesas), validação de integridade entre tipos de transação e categorias, e a lógica de geração automática de parcelas para compras no cartão de crédito.

## 2. Invariantes e Regras de Negócio

### 2.1. Validações Gerais
- `amountInCents` deve ser estritamente maior que zero (`amountInCents > 0`).
- A `Category` associada deve possuir o mesmo tipo da transação:
  - Transação `INCOME` só aceita categoria com `CategoryType.INCOME`.
  - Transação `EXPENSE` só aceita categoria com `CategoryType.EXPENSE`.
- A flag `isEssential` aplica-se apenas a despesas. Para transações do tipo `INCOME`, o valor deve ser sempre forçado para `false`.

### 2.2. Regras de Parcelamento (Cartão de Crédito)
- Transações com `installmentsCount > 1` só são permitidas se `paymentMethod == PaymentMethod.CREDIT_CARD`.
- Caso `installmentsCount == 1`, trata-se de compra à vista ou débito/dinheiro/PIX normal.
- **Algoritmo de Divisão de Parcelas:**
  - O usuário informa o **valor total da compra** e o **número de parcelas ($N$)**.
  - O valor base de cada parcela é: `baseAmount = totalInCents / N`.
  - O resto da divisão (`remainder = totalInCents % N`) deve ser somado integralmente na **primeira parcela**, evitando perda de centavos.
  - Exemplo: R$ 100,00 (10000 centavos) em 3x:
    - Parcela 1: 3334 centavos (R$ 33,34)
    - Parcela 2: 3333 centavos (R$ 33,33)
    - Parcela 3: 3333 centavos (R$ 33,33)
- **Datas das Parcelas:**
  - A parcela 1 assume a data base informada (`timestamp`).
  - As parcelas subsequentes ($2$ a $N$) são geradas com intervalo de 1 mês (`plusMonths(index - 1)`), mantendo o mesmo dia do mês (ou ajustado para o último dia caso o mês de destino tenha menos dias, ex: 31 de janeiro -> 28/29 de fevereiro).

## 3. Contratos de Use Cases (Camada de Domínio)

```kotlin
// Input DTO para criação de transação
data class CreateTransactionParams(
    val amountInCents: Long,
    val timestamp: Long,
    val categoryId: Long,
    val type: TransactionType,
    val paymentMethod: PaymentMethod,
    val isEssential: Boolean = false,
    val installmentsCount: Int = 1,
    val notes: String? = null
)

interface CreateTransactionUseCase {
    suspend operator fun invoke(params: CreateTransactionParams): Result<List<Long>>
}

interface DeleteTransactionUseCase {
    suspend operator fun invoke(transactionId: Long): Result<Unit>
}

interface GetTransactionsUseCase {
    operator fun invoke(startDate: Long, endDate: Long): Flow<List<Transaction>>
}
