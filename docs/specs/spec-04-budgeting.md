# Spec 04: Planejamento Orçamentário e Metas Mensais

## 1. Visão Geral
Esta especificação define o módulo de planejamento financeiro mensal (orçamentos/teto de gastos). O usuário pode estipular limites previstos para categorias específicas ou para o total de despesas de um mês de referência, permitindo comparar o planejado contra o executado sem alterar os registros reais de transação.

## 2. Modelagem de Dados

### 2.1. MonthlyBudgetEntity (`monthly_budgets`)
Tabela responsável por registrar o teto orçado por categoria para um determinado mês/ano:
- `id`: Long (PK, autoGenerate = true)
- `year`: Int (Not Null, ex: 2026)
- `month`: Int (Not Null, 1 a 12)
- `categoryId`: Long (FK -> `categories.id`, onDelete = CASCADE)
- `plannedAmountInCents`: Long (Not Null, deve ser > 0)

**Índice Único:**
- `UNIQUE(year, month, categoryId)` para evitar múltiplos orçamentos para a mesma categoria no mesmo período.

## 3. Invariantes e Regras de Negócio

### 3.1. Isolamento dos Dados Reais
- O planejamento nunca altera, deduz ou insere transações reais (`transactions`).
- Apenas categorias do tipo `EXPENSE` podem receber tetos orçamentários. Categorias do tipo `INCOME` ou `INVESTMENT` devem ser rejeitadas.

### 3.2. Cálculo do Comparativo (Planejado vs. Executado)
Para um determinado mês (`year`, `month`):
- O total gasto real em cada categoria (`actualSpentInCents`) é a soma de todas as transações com:
  - `type == EXPENSE`
  - `categoryId == budget.categoryId`
  - Data dentro do primeiro e do último milissegundo do mês informado.
- **Métricas calculadas:**
  $$\text{remainingInCents} = \text{plannedAmountInCents} - \text{actualSpentInCents}$$
  $$\text{percentageUsed} = \left(\frac{\text{actualSpentInCents}}{\text{plannedAmountInCents}}\right) \times 100$$
- Uma categoria é considerada estourada (`isExceeded = true`) quando $\text{actualSpentInCents} > \text{plannedAmountInCents}$.

### 3.3. Reutilização de Planejamento (Clonagem de Mês)
- O usuário pode copiar todas as metas do mês atual para o mês seguinte com um único comando.
- Se o mês de destino já possuir metas cadastradas, a operação deve permitir sobreescrita ou ignorar duplicatas conforme parâmetro informado (`overrideExisting: Boolean`).

## 4. Contratos de Use Cases (Camada de Domínio)

```kotlin
data class SetBudgetParams(
    val year: Int,
    val month: Int,
    val categoryId: Long,
    val plannedAmountInCents: Long
)

data class CategoryBudgetComparison(
    val categoryId: Long,
    val categoryName: String,
    val plannedAmountInCents: Long,
    val actualSpentInCents: Long,
    val remainingInCents: Long,
    val percentageUsed: Double,
    val isExceeded: Boolean
)

data class MonthlyBudgetOverview(
    val year: Int,
    val month: Int,
    val totalPlannedInCents: Long,
    val totalSpentInCents: Long,
    val totalRemainingInCents: Long,
    val categoriesComparison: List<CategoryBudgetComparison>
)

interface SetCategoryBudgetUseCase {
    suspend operator fun invoke(params: SetBudgetParams): Result<Long>
}

interface RemoveCategoryBudgetUseCase {
    suspend operator fun invoke(budgetId: Long): Result<Unit>
}

interface GetMonthlyBudgetOverviewUseCase {
    operator fun invoke(year: Int, month: Int): Flow<MonthlyBudgetOverview>
}

interface CopyBudgetUseCase {
    suspend operator fun invoke(
        fromYear: Int,
        fromMonth: Int,
        toYear: Int,
        toMonth: Int,
        overrideExisting: Boolean = false
    ): Result<Int> // Retorna a quantidade de metas clonadas
}
```

## 5. Critérios de Aceite (Testes Unitários Obrigatórios)

- [ ] **Teste 1:** Falhar ao criar orçamento para categoria do tipo `INCOME` ou `INVESTMENT`.
- [ ] **Teste 2:** Falhar se `plannedAmountInCents <= 0`.
- [ ] **Teste 3:** Inserir dois orçamentos para a mesma `(year, month, categoryId)` deve resultar em upsert (atualização), não duplicata.
- [ ] **Teste 4:** `isExceeded = true` quando `actualSpentInCents > plannedAmountInCents`.
- [ ] **Teste 5:** `remainingInCents` deve ser negativo quando o orçamento é estourado.
- [ ] **Teste 6:** `CopyBudgetUseCase` com `overrideExisting = false` não deve sobrescrever metas já existentes no mês destino; deve ignorar silenciosamente as duplicatas e retornar apenas a contagem de itens novos inseridos.
- [ ] **Teste 7:** `CopyBudgetUseCase` com `overrideExisting = true` deve substituir metas existentes e retornar a contagem total copiada.
- [ ] **Teste 8:** Copiar de um mês vazio (sem metas) deve retornar `Result.success(0)` sem erros.
