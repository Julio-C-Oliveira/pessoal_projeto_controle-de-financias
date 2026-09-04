# Tarefa 05: Implementação de Relatórios, Agregações e Telas Compose

## Instruções de Execução
1. Consulte `docs/specs/spec-05-ui-reports.md` e as regras de `.antigravity/rules.md`.
2. Adicione a dependência `androidx.navigation:navigation-compose` no `build.gradle.kts` caso não esteja presente.
3. Crie o caso de uso `GetFinancialReportUseCase` no pacote `domain/usecase/report/`:
   - Implemente o particionamento de datas (`java.time.LocalDate`, `temporal.TemporalAdjusters`).
   - Calcule as métricas de despesas essenciais, não essenciais e totais por categoria.
4. Crie os testes unitários para a lógica de agregação em `test/domain/usecase/report/GetFinancialReportUseCaseTest.kt`.
5. Implemente a interface gráfica em `ui/`:
   - `ui/navigation/`: Estrutura de rotas (`NavHost`) e `NavigationBar` Material 3.
   - `ui/screens/dashboard/`: `DashboardScreen`, `DashboardViewModel`, `DashboardUiState`.
   - `ui/screens/transactions/`: Telas de extrato e formulário de nova transação.
   - `ui/screens/budget/`: Visualização das metas da Spec 04.
   - `ui/screens/investments/`: Telas de ativos e aportes da Spec 03.
   - `ui/screens/reports/`: Seletor temporal e gráficos/barras de proporção em Compose puro (Canvas ou Row com weight).
6. Execute `./gradlew testDebugUnitTest` e valide a compilação do projeto com `./gradlew assembleDebug`.
