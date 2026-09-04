# Tarefa 01: Setup do Room e Implementação da Persistência

## Instruções de Execução
1. Leia as regras em `.antigravity/rules.md` e os requisitos em `docs/specs/spec-01-database.md`.
2. Adicione as dependências do Room (`androidx.room:room-runtime`, `androidx.room:room-ktx` e plugin KSP) no `build.gradle.kts` caso ainda não estejam configuradas.
3. Crie os arquivos dentro do pacote `data/local/`:
   - Enums e TypeConverters necessários.
   - `CategoryEntity.kt`, `TransactionEntity.kt`, `InvestmentEntity.kt`.
   - `CategoryDao.kt`, `TransactionDao.kt`, `InvestmentDao.kt`.
   - `AppDatabase.kt` configurado com version = 1.
4. Implemente os modelos de domínio e a interface `FinanceRepository` com sua implementação `FinanceRepositoryImpl`.
5. Crie a suíte de testes unitários para o Room em `src/test` (utilizando Robolectric) ou `src/androidTest`.
6. Valide a compilação e execute a suíte de testes.
