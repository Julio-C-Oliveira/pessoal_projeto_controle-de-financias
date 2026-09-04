# Tarefa 04: Implementação do Módulo de Orçamento e Metas

## Instruções de Execução
1. Consulte `docs/specs/spec-04-budgeting.md` e as regras de `.antigravity/rules.md`.
2. Atualize a camada de persistência local (`data/local`):
   - Adicione `MonthlyBudgetEntity` com índice único composto em `(year, month, categoryId)`.
   - Adicione `MonthlyBudgetDao` com operações de inserção em lote, upsert e busca por período.
   - Atualize a versão do `AppDatabase`.
3. Atualize o `FinanceRepository` para expor o acesso a orçamentos e à junção de dados com transações.
4. Crie os use cases no pacote `domain/usecase/budget/`:
   - `SetCategoryBudgetUseCase`
   - `RemoveCategoryBudgetUseCase`
   - `GetMonthlyBudgetOverviewUseCase`
   - `CopyBudgetUseCase`
5. Crie a suíte de testes unitários em `src/test/java/.../domain/usecase/budget/`:
   - `MonthlyBudgetTest`: cobrindo todos os critérios de aceite da Spec 04.
6. Execute `./gradlew testDebugUnitTest` e valide se todos os testes continuam passando.
