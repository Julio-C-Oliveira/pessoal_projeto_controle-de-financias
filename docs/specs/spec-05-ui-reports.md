# Spec 05: Agregações Temporais, Relatórios e UI em Jetpack Compose

## 1. Visão Geral
Esta especificação define o motor de consolidação analítica de dados (visões semanal, mensal e anual) e a arquitetura da interface de usuário com Jetpack Compose (Material 3). A UI segue o padrão Unidirectional Data Flow (UDF), consumindo `StateFlow` dos ViewModels e emitindo eventos sem retenção de estado de negócio nos componentes visuais.

## 2. Motor de Agregações e Métricas

### 2.1. Escopos Temporais (`TimePeriod`)
- `WEEK`: Segunda-feira 00:00:00 até Domingo 23:59:59 da semana de referência.
- `MONTH`: Primeiro dia 00:00:00 até o último dia 23:59:59 do mês selecionado.
- `YEAR`: 1º de Janeiro 00:00:00 até 31 de Dezembro 23:59:59 do ano selecionado.

### 2.2. Fórmulas de Consolidação
Para qualquer período consultado:
- **Total de Receitas ($R$):** $\sum \text{amountInCents}$ de todas as transações `INCOME`.
- **Total de Despesas ($D$):** $\sum \text{amountInCents}$ de todas as transações `EXPENSE`.
- **Total de Despesas Essenciais ($D_E$):** $\sum \text{amountInCents}$ com `isEssential == true`.
- **Total de Despesas Não Essenciais ($D_{NE}$):** $\sum \text{amountInCents}$ com `isEssential == false`.
- **Total Aportado em Investimentos ($I$):** $\sum \text{amountInCents}$ das contribuições cadastradas no período.
- **Saldo Líquido Remanescente ($S$):**
  $$S = R - D$$
- **Capacidade de Poupança / Caixa Livre ($C$):**
  $$C = R - D - I$$

## 3. Contratos de Domínio (Relatórios)

```kotlin
enum class PeriodType { WEEK, MONTH, YEAR }

data class TimeFilter(
    val periodType: PeriodType,
    val referenceDate: Long // Timestamp contido no período desejado
)

data class CategoryExpenseSummary(
    val categoryId: Long,
    val categoryName: String,
    val totalInCents: Long,
    val percentageOfTotal: Double
)

data class PeriodFinancialReport(
    val filter: TimeFilter,
    val totalIncomeInCents: Long,
    val totalExpenseInCents: Long,
    val essentialExpenseInCents: Long,
    val nonEssentialExpenseInCents: Long,
    val netBalanceInCents: Long,
    val totalInvestedInCents: Long,
    val freeCashInCents: Long,
    val categoryExpenses: List<CategoryExpenseSummary>
)

interface GetFinancialReportUseCase {
    operator fun invoke(filter: TimeFilter): Flow<PeriodFinancialReport>
}
```

## 4. Decisões de Design

- **Sem paginação:** `GetTransactionsUseCase` retorna `Flow<List<Transaction>>` sem `PagingSource`. O volume de dados de uma pessoa física em uso pessoal é pequeno o suficiente para não justificar a complexidade de paginação. Caso o histórico cresça, essa decisão deve ser revisitada e a spec atualizada.
- **Semana começa na segunda-feira:** O escopo `WEEK` usa segunda-feira como início de semana (padrão ISO 8601). Ajuste via `TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)`.

## 5. Critérios de Aceite (Testes Unitários Obrigatórios)

- [ ] **Teste 1:** `PeriodType.WEEK` — relatório gerado com `referenceDate` numa quinta-feira deve incluir apenas transações de segunda a domingo daquela semana.
- [ ] **Teste 2:** `PeriodType.MONTH` — relatório de fevereiro não deve incluir transações de janeiro ou março.
- [ ] **Teste 3:** `netBalanceInCents = totalIncomeInCents - totalExpenseInCents` calculado corretamente.
- [ ] **Teste 4:** `freeCashInCents = netBalanceInCents - totalInvestedInCents` calculado corretamente.
- [ ] **Teste 5:** A soma de `percentageOfTotal` de todas as entradas em `categoryExpenses` deve ser igual a 100.0 (tolerância de 0.01 por arredondamento).
- [ ] **Teste 6:** `essentialExpenseInCents + nonEssentialExpenseInCents == totalExpenseInCents` sempre verdadeiro.
