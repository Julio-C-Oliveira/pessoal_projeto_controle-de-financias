# Tarefa 08: Implementação de Receitas e Despesas Recorrentes

## Instruções de Execução

1. Consulte `docs/specs/spec-08-recurring-transactions.md` e as regras do projeto antes de iniciar.
2. Siga rigorosamente a ordem das etapas descritas abaixo.

---

### Etapa 1 — Camada de Dados (Database, Enums, Entities e DAOs)

1. Crie o enum `RecurrenceFrequency` em `data/local/enums/RecurrenceFrequency.kt`:
   - Valores: `MONTHLY`, `WEEKLY`, `YEARLY`.
2. Adicione o campo `recurringTransactionId: Long? = null` na entidade `TransactionEntity` (`data/local/entity/TransactionEntity.kt`).
3. Crie a nova entidade `RecurringTransactionEntity` em `data/local/entity/RecurringTransactionEntity.kt`:
   - Tabela: `"recurring_transactions"`
   - Campos: `id`, `amountInCents`, `categoryId`, `type`, `paymentMethod`, `frequency`, `startDate`, `endDate: Long?`, `totalOccurrences: Int?`, `generatedCount: Int`, `lastGeneratedDate: Long?`, `isActive: Boolean = true`, `isEssential: Boolean = false`, `notes: String?`.
   - Foreign key para `categories.id` (`onDelete = RESTRICT`).
4. Crie a DAO `RecurringTransactionDao` em `data/local/dao/RecurringTransactionDao.kt`:
   - `fun getAllRecurringTransactions(): Flow<List<RecurringTransactionEntity>>`
   - `fun getActiveRecurringTransactions(): Flow<List<RecurringTransactionEntity>>`
   - `suspend fun insert(recurring: RecurringTransactionEntity): Long`
   - `suspend fun update(recurring: RecurringTransactionEntity)`
   - `suspend fun deleteById(id: Long)`
5. Atualize o `AppDatabase` (`data/local/AppDatabase.kt`):
   - Registre `RecurringTransactionEntity` nas `@Database(entities = [...])`.
   - Incremente a versão do banco (v6).
   - Exponha `abstract fun recurringTransactionDao(): RecurringTransactionDao`.

---

### Etapa 2 — Camada de Repositório e Modelos de Domínio

1. Crie a data class `RecurringTransaction` em `domain/model/Models.kt`:
   - ID, amountInCents, categoryId, category resolvida, type, paymentMethod, frequency, startDate, endDate, totalOccurrences, generatedCount, lastGeneratedDate, isActive, isEssential, notes.
2. Adicione métodos à interface `FinanceRepository` (`domain/repository/FinanceRepository.kt`):
   - `fun getRecurringTransactions(): Flow<List<RecurringTransaction>>`
   - `suspend fun insertRecurringTransaction(recurring: RecurringTransactionEntity): Long`
   - `suspend fun updateRecurringTransaction(recurring: RecurringTransactionEntity)`
   - `suspend fun deleteRecurringTransaction(id: Long)`
   - `suspend fun getActiveRecurringEntities(): List<RecurringTransactionEntity>`
3. Implemente os métodos no `FinanceRepositoryImpl` (`data/repository/FinanceRepositoryImpl.kt`).

---

### Etapa 3 — Camada de Domínio (Use Cases)

1. Crie o pacote `domain/usecase/recurring/` e implemente os seguintes Use Cases:
   - `CreateRecurringTransactionUseCase` (com suporte a `totalOccurrences`, `amountInCents > 0`, validação de categorias e `isEssential`).
   - `GetRecurringTransactionsUseCase`.
   - `ToggleRecurringTransactionUseCase`.
   - `DeleteRecurringTransactionUseCase`.
   - `ProcessDueRecurringTransactionsUseCase` (algoritmo de cálculo de ciclos devidos por `frequency`, limite de `totalOccurrences`, controle por `generatedCount` e geração de lançamentos em `transactions`).

---

### Etapa 4 — Testes Unitários

1. Crie a classe de teste `CreateRecurringTransactionUseCaseTest` em `test/domain/usecase/recurring/`.
2. Crie a classe de teste `ProcessDueRecurringTransactionsUseCaseTest` em `test/domain/usecase/recurring/`.
3. Valide todos os Critérios de Aceite (AC-01 a AC-08 + AC-09 Extra de `totalOccurrences`) em `docs/specs/spec-08-recurring-transactions.md`.
4. Execute `./gradlew testDebugUnitTest` para garantir que todos os testes passem.

---

### Etapa 5 — Camada de Apresentação (UI Compose & ViewModel)

1. Atualize o `TransactionsViewModel` para expor:
   - Lista de regras recorrentes ativas.
   - Ações de criação (com `timestamp` e `totalOccurrences`), alteração de estado e exclusão.
   - Chamada automática ao `ProcessDueRecurringTransactionsUseCase` durante o carregamento da tela.
2. Atualize a UI de `TransactionsScreen.kt`:
   - Adicione checkbox "Tornar Recorrente" no diálogo de adicionar transação.
   - Adicione seletor de data da transação ("Hoje" vs "Data Específica").
   - Adicione seletor de dia da semana/mês/ano da recorrência.
   - Adicione seletor de duração ("Tempo Indeterminado" vs "Período Específico" com atalhos em 2 linhas e campo livre).
   - Adicione visualização de progresso nos cards de recorrência ("Desde DD/MM/AAAA (X/Y ocorrências)").

---

### Etapa 6 — Verificação Final

1. Execute `./gradlew testDebugUnitTest` e confirme 100% dos testes verdes.
2. Execute `./gradlew assembleDebug` para confirmar que a compilação do APK está perfeita.
3. Atualize o status da spec em `docs/specs/spec-00-overview.md`.

