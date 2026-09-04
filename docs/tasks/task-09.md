# Tarefa 09: Atualização do Backup e Restauração Local (Payload v2)

## Instruções de Execução

1. Consulte `docs/specs/spec-09-backup-update.md` e as regras do projeto em `.antigravity/rules.md` antes de iniciar.
2. Siga rigorosamente a ordem das etapas descritas abaixo.

---

### Etapa 1 — Atualização dos DTOs e Mappers (`data/local/backup/`)

1. Em `data/local/backup/BackupPayloadDto.kt`:
   - Crie a data class `@Serializable data class RecurringTransactionBackupDto(...)` com todos os campos da entidade `RecurringTransactionEntity`.
   - Adicione `val recurringTransactionId: Long? = null` em `TransactionBackupDto`.
   - Atualize `BackupPayloadDto` alterando a versão padrão para `version: Int = 2` e adicionando o campo `val recurringTransactions: List<RecurringTransactionBackupDto> = emptyList()`.
2. Em `data/local/backup/BackupMappers.kt`:
   - Crie as funções de extensão `RecurringTransactionEntity.toBackupDto()` e `RecurringTransactionBackupDto.toEntity()`.
   - Atualize `TransactionEntity.toBackupDto()` para repassar `recurringTransactionId`.
   - Atualize `TransactionBackupDto.toEntity()` para repassar `recurringTransactionId`.

---

### Etapa 2 — Atualização da Camada de Repositório (`data/repository/FinanceRepositoryImpl.kt`)

1. Atualize o método `exportBackup()` em `FinanceRepositoryImpl`:
   - Inclua a leitura de `recurringTransactionDao?.getAllRecurringTransactions()?.first()?.map { it.toBackupDto() } ?: emptyList()`.
   - Construa o `BackupPayloadDto` com `version = 2` e a lista `recurringTransactions`.
2. Atualize o método `importBackup()` em `FinanceRepositoryImpl`:
   - Adicione a exclusão de `recurringTransactionDao?.deleteAllRecurringTransactions()` (ou método equivalente no DAO) na ordem inversa de Foreign Keys, antes de excluir as categorias.
   - Adicione a inserção de `payload.recurringTransactions.map { it.toEntity() }` respeitando a ordem de Foreign Keys (após `categories` e antes de `transactions`).

---

### Etapa 3 — Atualização do Use Case de Importação (`domain/usecase/backup/`)

1. Atualize `ImportBackupUseCaseImpl` em `domain/usecase/backup/ImportBackupUseCase.kt`:
   - Altere a validação de versão para aceitar `payload.version <= 2`.
   - Lance `UnsupportedBackupVersionException` apenas se `payload.version > 2`.

---

### Etapa 4 — Atualização e Execução dos Testes (`test/data/local/backup/BackupIntegrationTest.kt`)

1. Em `BackupIntegrationTest.kt`:
   - Atualize a fixtura `seedDatabase()` para inserir uma `RecurringTransactionEntity` e vincular seu ID a uma `TransactionEntity` via `recurringTransactionId`.
   - Atualize `testFullBackupAndRestoreCyclePreservesDataAndIds()` para asserir que a `RecurringTransactionEntity` e o `recurringTransactionId` foram restaurados com sucesso.
   - Adicione `testImportBackupVersion1Retrocompatibility()`, garantindo que JSONs de backup da `version = 1` sem a chave `recurringTransactions` sejam restaurados com sucesso.
   - Atualize `testImportBackupWithUnsupportedVersionFails()` para testar `version = 3`.
   - Atualize `testExportedJsonContainsAllRequiredKeys()` para validar `version = 2` e a presença da chave `recurringTransactions`.
2. Execute `./gradlew testDebugUnitTest` e certifique-se de que todos os testes passem com 100% de sucesso.

---

### Etapa 5 — Verificação Final e Atualização da Documentação

1. Execute `./gradlew testDebugUnitTest` e confirme 100% dos testes verdes.
2. Execute `./gradlew assembleDebug` para confirmar que a compilação do APK está sem erros.
3. Atualize o status da spec em `docs/specs/spec-00-overview.md`.
