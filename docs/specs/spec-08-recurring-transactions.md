# Spec 08: Receitas e Despesas Recorrentes

## 1. Visão Geral
Esta especificação define o modelo e as regras de negócio para a gestão de receitas e despesas recorrentes (ex: salários, aluguel, mensalidades, assinaturas). O sistema permite cadastrar regras de recorrência periódicas (indeterminadas ou por período específico de ocorrências), com agendamento do dia/data de execução e geração automática das transações reais correspondentes à medida que o tempo avança.

## 2. Invariantes e Regras de Negócio

### 2.1. Frequências de Recorrência
- `RecurrenceFrequency`: Enum com os valores:
  - `MONTHLY`: Recorrência mensal (ex: no dia 5 de cada mês).
  - `WEEKLY`: Recorrência semanal (ex: toda segunda-feira).
  - `YEARLY`: Recorrência anual (ex: no mesmo dia e mês a cada ano).

### 2.2. Validações Gerais e Parâmetros da Regra de Recorrência
- `amountInCents` deve ser estritamente maior que zero (`amountInCents > 0`).
- A `Category` associada deve possuir o mesmo tipo da transação recorrente:
  - Transação `INCOME` só aceita categoria com `CategoryType.INCOME`.
  - Transação `EXPENSE` só aceita categoria com `CategoryType.EXPENSE`.
- A flag `isEssential` aplica-se apenas a despesas. Para transações recorrentes do tipo `INCOME`, o valor deve ser sempre forçado para `false`.
- **Seleção de Data Inicial e Dia da Recorrência:**
  - O usuário pode optar por iniciar **Hoje** ou em uma **Data Específica** (`DD/MM/AAAA` ou `MM/AAAA`).
  - **Frequência Semanal:** O usuário define o dia da semana (`Seg`, `Ter`, `Qua`, `Qui`, `Sex`, `Sáb`, `Dom`).
  - **Frequência Mensal:** O usuário define o dia do mês (1 a 31).
  - **Frequência Anual:** O usuário define o dia e mês (`DD/MM`, ex: `25/12`).
- **Duração e Limite de Ocorrências (`totalOccurrences`):**
  - **Tempo Indeterminado:** `totalOccurrences == null` (gera indefinidamente).
  - **Período Específico:** `totalOccurrences` é um inteiro positivo > 1 (ex: 2, 3, 4, 5, 12 ou qualquer valor personalizado). O campo `generatedCount` rastreia quantos lançamentos já foram gerados.

### 2.3. Algoritmo de Processamento e Geração Automática
- Sempre que a funcionalidade de verificação for invocada (`ProcessDueRecurringTransactionsUseCase`), o sistema busca todas as regras de recorrência onde `isActive == true`.
- Para cada regra ativa:
  - Enquanto a próxima data do ciclo for menor ou igual à data/timestamp atual (`currentTimestamp`), respeitando a data final (`endDate`) e o limite de ocorrências (`generatedCount < totalOccurrences` se `totalOccurrences != null`):
    1. Cria uma nova `TransactionEntity` na tabela `transactions` com os mesmos dados da regra (`amountInCents`, `categoryId`, `type`, `paymentMethod`, `isEssential`, `notes`).
    2. A data (`timestamp`) da transação criada assume a data calculada do ciclo.
    3. Vincula o ID da regra no campo `recurringTransactionId` da nova transação.
    4. Incrementa `generatedCount` e atualiza `lastGeneratedDate` da regra para a data desse lançamento recém-gerado.

### 2.4. Ciclos e Ajuste de Dias
- Para `MONTHLY`: Mantém o dia do mês definido pelo usuário. Caso o mês de destino tenha menos dias que o dia original (ex: dia 31 em fevereiro), ajusta para o último dia do mês (ex: 28/29 de fevereiro).
- Para `WEEKLY`: Adiciona exatamente 7 dias (`plusWeeks(1)`).
- Para `YEARLY`: Adiciona 1 ano (`plusYears(1)`).

### 2.5. Status e Gerenciamento
- **Ativação / Pausa (`isActive`):** Se `isActive == false`, a regra não gera novas transações durante o processamento, mantendo o `lastGeneratedDate` congelado.
- **Exclusão de Regra:** Ao excluir uma regra de recorrência, os lançamentos passados já gerados na tabela `transactions` permanecem intactos.

---

## 3. Contratos de Use Cases (Camada de Domínio)

```kotlin
enum class RecurrenceFrequency {
    MONTHLY,
    WEEKLY,
    YEARLY
}

data class CreateRecurringTransactionParams(
    val amountInCents: Long,
    val categoryId: Long,
    val type: TransactionType,
    val paymentMethod: PaymentMethod,
    val frequency: RecurrenceFrequency,
    val startDate: Long,
    val endDate: Long? = null,
    val totalOccurrences: Int? = null,
    val isEssential: Boolean = false,
    val notes: String? = null
)

interface CreateRecurringTransactionUseCase {
    suspend operator fun invoke(params: CreateRecurringTransactionParams): Result<Long>
}

interface GetRecurringTransactionsUseCase {
    operator fun invoke(): Flow<List<RecurringTransaction>>
}

interface ToggleRecurringTransactionUseCase {
    suspend operator fun invoke(recurringId: Long, isActive: Boolean): Result<Unit>
}

interface DeleteRecurringTransactionUseCase {
    suspend operator fun invoke(recurringId: Long): Result<Unit>
}

interface ProcessDueRecurringTransactionsUseCase {
    suspend operator fun invoke(currentTimestamp: Long): Result<Int> // Retorna a quantidade de transações geradas
}
```

---

## 4. Decisões de Design e UI/UX

- **Entidade Separada (`RecurringTransactionEntity`):** Tabela `"recurring_transactions"` com suporte a `totalOccurrences` e `generatedCount`.
- **Layout de Seleção Spacioso:** Chips de atalho de repetição em múltiplas linhas e seletor de dia da semana/mês/ano dedicado.
- **Idempotência de Geração:** O controle por `lastGeneratedDate` e `generatedCount` garante que o processamento possa rodar a qualquer momento sem duplicar lançamentos ou ultrapassar o limite de ocorrências.

---

## 5. Critérios de Aceite (Testes Unitários Obrigatórios)

- [x] **Teste 1:** Falhar com erro ao tentar criar transação recorrente com `amountInCents <= 0`.
- [x] **Teste 2:** Falhar se a categoria associada possuir tipo incompatível com a transação recorrente (ex: `INCOME` com categoria de despesa).
- [x] **Teste 3:** Forçar `isEssential = false` para transações recorrentes do tipo `INCOME`, mesmo que o caller envie `true`.
- [x] **Teste 4:** Verificar que `ProcessDueRecurringTransactionsUseCase` gera corretamente lançamentos devidos no período. Ex: Regra mensal iniciada há 2 meses e nunca processada deve gerar exatamente 3 transações (mês 0, mês 1 e mês 2) e atualizar `lastGeneratedDate`.
- [x] **Teste 5:** Garantir que regras inativas (`isActive = false`) são ignoradas pelo `ProcessDueRecurringTransactionsUseCase`.
- [x] **Teste 6:** Verificar ajuste do último dia do mês para frequência `MONTHLY` em meses curtos (ex: 31 de janeiro -> 28/29 de fevereiro).
- [x] **Teste 7:** `ToggleRecurringTransactionUseCase` altera com sucesso o estado de `isActive`.
- [x] **Teste 8:** `DeleteRecurringTransactionUseCase` remove a regra de recorrência sem excluir as transações reais previamente geradas.
- [x] **Teste 9 (Extra):** Garantir que a regra interrompe a geração de transações assim que `generatedCount` atinge `totalOccurrences`.

