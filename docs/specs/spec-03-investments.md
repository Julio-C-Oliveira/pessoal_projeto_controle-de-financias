# Spec 03: Gestão de Ativos, Aportes e Patrimônio

## 1. Visão Geral
Esta especificação define as regras de negócio para o acompanhamento de investimentos (Renda Fixa e Variável). O módulo permite cadastrar ativos, registrar aportes financeiros com histórico temporal e atualizar manualmente a posição patrimonial (saldo atualizado).

## 2. Modelagem e Entidades Adicionais

Para manter o histórico de aportes sem depender apenas da atualização do saldo, adiciona-se uma entidade de contribuição vinculada ao ativo:

### 2.1. InvestmentContributionEntity (`investment_contributions`)
- `id`: Long (PK, autoGenerate = true)
- `investmentId`: Long (FK -> `investments.id`, onDelete = CASCADE)
- `amountInCents`: Long (Not Null, deve ser > 0)
- `timestamp`: Long (Epoch millis, Not Null)
- `notes`: String? (Opcional)

### 2.2. Enums de Domínio
- `InvestmentHorizon`: `SHORT` (Curto prazo), `MEDIUM` (Médio prazo), `LONG` (Longo prazo) — mapeado para `String` no banco via `TypeConverter`.
- `InvestmentType`: `FIXED_INCOME`, `VARIABLE`

## 3. Invariantes e Regras de Negócio

### 3.1. Ativos de Investimento
- O nome do ativo não pode ser vazio ou conter apenas espaços em branco.
- O campo `currentBalanceInCents` não pode ser negativo (`currentBalanceInCents >= 0`).
- Ao cadastrar um ativo com saldo inicial $> 0$, deve ser criado automaticamente um primeiro registro em `investment_contributions` com o valor inicial e o timestamp da criação.

### 3.2. Aportes (Contribuições)
- O valor do aporte (`amountInCents`) deve ser estritamente maior que zero.
- Todo aporte registrado para um investimento existente incrementa automaticamente o `currentBalanceInCents` do ativo pelo mesmo montante:
  $$\text{newBalance} = \text{currentBalance} + \text{amount}$$
- O histórico de aportes deve ser retornado em ordem decrescente de data (`timestamp DESC`).

### 3.3. Atualização de Saldo (Rendimento/Variação de Mercado)
- Como o app é 100% offline e não consome APIs de cotação ou taxas (CDI/Selic), o usuário pode atualizar o saldo atual a qualquer momento.
- A atualização do saldo patrimonial **não** altera o histórico de aportes já registrados, permitindo calcular o rendimento acumulado:
  $$\text{rendimento} = \text{currentBalanceInCents} - \sum \text{aportes}$$

## 4. Contratos de Use Cases (Camada de Domínio)

```kotlin
data class CreateInvestmentParams(
    val name: String,
    val type: InvestmentType,
    val horizon: InvestmentHorizon,
    val initialAmountInCents: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

data class AddContributionParams(
    val investmentId: Long,
    val amountInCents: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)

interface CreateInvestmentUseCase {
    suspend operator fun invoke(params: CreateInvestmentParams): Result<Long>
}

interface AddContributionUseCase {
    suspend operator fun invoke(params: AddContributionParams): Result<Long>
}

interface UpdateInvestmentBalanceUseCase {
    suspend operator fun invoke(investmentId: Long, newBalanceInCents: Long): Result<Unit>
}

interface GetInvestmentsSummaryUseCase {
    operator fun invoke(): Flow<InvestmentSummary>
}

data class InvestmentSummary(
    val totalInvestedInCents: Long,       // Soma de todos os saldos atuais
    val totalContributionsInCents: Long,  // Soma de todos os aportes históricos
    val totalYieldInCents: Long,          // totalInvested - totalContributions
    val investments: List<InvestmentDetail>
)

data class InvestmentDetail(
    val id: Long,
    val name: String,
    val type: InvestmentType,
    val horizon: InvestmentHorizon,
    val currentBalanceInCents: Long,
    val totalAportadoInCents: Long
)

## 5. Decisões de Design

- **Atualização manual de saldo não é um aporte:** O `UpdateInvestmentBalanceUseCase` permite ao usuário refletir a posição atual de mercado (ex: cotação do fundo atualizada) sem registrar entrada de dinheiro novo. Isso mantém a separação clara entre rendimento (variação do valor) e capital aportado (dinheiro novo adicionado), permitindo calcular o rendimento real: $\text{rendimento} = \text{saldoAtual} - \sum\text{aportes}$.
- **Saldo inicial como aporte automático:** Cadastrar um ativo com saldo inicial $> 0$ gera um `InvestmentContribution` automático para manter a consistência do histórico desde o primeiro dia.

## 6. Critérios de Aceite (Testes Unitários Obrigatórios)

- [ ] **Teste 1:** Falhar ao criar ativo com nome vazio ou contendo apenas espaços em branco.
- [ ] **Teste 2:** Criar ativo com `initialAmountInCents = 1000` deve gerar automaticamente 1 registro em `InvestmentContribution` com o mesmo valor e timestamp.
- [ ] **Teste 3:** Criar ativo com `initialAmountInCents = 0` não deve gerar nenhum `InvestmentContribution`.
- [ ] **Teste 4:** `AddContributionUseCase` deve incrementar `currentBalanceInCents` pelo valor exato do aporte.
- [ ] **Teste 5:** Falhar se `amountInCents <= 0` em `AddContributionUseCase`.
- [ ] **Teste 6:** `UpdateInvestmentBalanceUseCase` atualiza o saldo sem alterar o histórico de aportes existentes.
- [ ] **Teste 7:** `GetInvestmentsSummaryUseCase` calcula `totalYieldInCents = totalInvested - totalContributions` corretamente.
