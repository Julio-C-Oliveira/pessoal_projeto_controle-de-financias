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
- **Agrupamento de Parcelas:**
  - Todas as parcelas de uma compra parcelada recebem o mesmo `installmentGroupId` (UUID v4 gerado internamente pelo use case no momento da criação).
  - Transações à vista (`installmentsCount == 1`) têm `installmentGroupId = null`.

### 2.3. Exclusão de Transações
- **Exclusão individual (padrão):** deleta apenas a parcela selecionada pelo `transactionId`.
- **Exclusão do grupo:** se `deleteEntireGroup = true` for passado e a transação possuir `installmentGroupId != null`, todas as parcelas do grupo são deletadas via `deleteTransactionsByGroupId`.

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
    // installmentGroupId é gerado internamente pelo use case; não é exposto como parâmetro de entrada
)

interface CreateTransactionUseCase {
    suspend operator fun invoke(params: CreateTransactionParams): Result<List<Long>>
}

interface DeleteTransactionUseCase {
    suspend operator fun invoke(
        transactionId: Long,
        deleteEntireGroup: Boolean = false
    ): Result<Unit>
}

interface GetTransactionsUseCase {
    operator fun invoke(startDate: Long, endDate: Long): Flow<List<Transaction>>
}
```

## 4. Decisões de Design

- **Resto na primeira parcela:** O centavo extra do resto da divisão inteira vai na parcela 1 (e não na última). Isso garante que o saldo devedor nunca fique a maior do que o valor real da compra durante o ciclo de pagamento.
- **Exclusão individual como padrão:** A exclusão única é o comportamento padrão do `DeleteTransactionUseCase` porque reflete melhor o caso de uso real (ex: uma parcela foi paga e precisa ser removida do extrato, mas as demais permanecem). A exclusão em grupo é uma opção explícita.
- **`isEssential` bloqueado em `INCOME`:** Forçar `isEssential = false` para receitas evita que a UI precise tratar esse campo como condicional por tipo, simplificando a lógica de filtragem nos relatórios.

## 5. Critérios de Aceite (Testes Unitários Obrigatórios)

- [ ] **Teste 1:** Falhar com erro se `amountInCents <= 0`.
- [ ] **Teste 2:** Falhar se a categoria associada for do tipo diferente da transação (ex: `INCOME` com categoria `EXPENSE`).
- [ ] **Teste 3:** `isEssential` deve ser forçado para `false` ao criar transação do tipo `INCOME`, mesmo que o caller passe `true`.
- [ ] **Teste 4:** Falhar se `installmentsCount > 1` e `paymentMethod != CREDIT_CARD`.
- [ ] **Teste 5:** Verificar algoritmo de parcelas — R$ 10,00 em 3x deve gerar [3334, 3333, 3333] centavos.
- [ ] **Teste 6:** Verificar datas geradas — compra em 31/01 em 3x deve gerar timestamps para 31/01, 28/02 e 31/03.
- [ ] **Teste 7:** Verificar que todas as parcelas geradas compartilham o mesmo `installmentGroupId` não-nulo.
- [ ] **Teste 8:** `DeleteTransactionUseCase` com `deleteEntireGroup = true` remove todas as parcelas do grupo.
