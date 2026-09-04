# Tarefa 03: Módulo de Investimentos e Histórico de Aportes

## Instruções de Execução
1. Consulte `docs/specs/spec-03-investments.md` e as regras de `.antigravity/rules.md`.
2. Atualize o esquema do Room em `data/local/`:
   - Adicione a tabela `InvestmentContributionEntity`.
   - Adicione o `InvestmentContributionDao` com métodos para inserir e listar contribuições por `investmentId`.
   - Incremente a versão do banco no `AppDatabase` (ou recrie caso esteja em desenvolvimento limpo).
3. Implemente no `FinanceRepository` os métodos necessários para suportar os use cases de investimentos.
4. Crie os use cases no pacote `domain/usecase/investment/`:
   - `CreateInvestmentUseCase`
   - `AddContributionUseCase`
   - `UpdateInvestmentBalanceUseCase`
   - `GetInvestmentsSummaryUseCase`
5. Implemente os testes unitários em `src/test/java/.../domain/usecase/investment/`:
   - Valide todos os critérios de aceite listados na Spec 03.
6. Execute `./gradlew testDebugUnitTest` e confirme que todos os testes passam sem erros.
