# App de Finanças Pessoal (Offline)

Aplicativo Android nativo focado em controle financeiro estritamente local, sem conexão externa ou dependência de servidores.

## Stack Técnica
- **Linguagem:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Arquitetura:** MVVM (Model-View-ViewModel) + StateFlow/Coroutines
- **Persistência:** Room (SQLite)
- **Configurações:** Jetpack DataStore Preferences

## Principais Módulos
1. **Transações:** Cadastro unificado de receitas e despesas com categorização, meio de pagamento e tag de essencialidade.
2. **Investimentos:** Controle manual de ativos, prazos, aportes e saldos.
3. **Planejamento:** Definição de metas orçamentárias mensais desvinculadas das transações reais.
4. **Relatórios:** Agrupamento temporal (semanal, mensal, anual) e comparativo de despesas essenciais vs. não essenciais.
