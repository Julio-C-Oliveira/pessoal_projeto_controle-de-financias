# Spec 00: Visão Geral, Mapa de Dependências e Glossário

## 1. Sobre o Projeto

Aplicativo Android nativo de controle financeiro **estritamente offline**. Nenhum dado sai do dispositivo. Veja o [README](../../README.md) para a stack técnica completa.

**Paradigma de desenvolvimento:** Spec-Driven Development (SDD) — toda implementação deve partir da leitura da spec correspondente e ser validada pelos seus Critérios de Aceite antes de avançar para a próxima tarefa.

---

## 2. Mapa de Dependências entre Specs

\`\`\`
spec-01 (Banco de Dados / Room)
    ├─► spec-02 (Transações)
    ├─► spec-03 (Investimentos)
    └─► spec-04 (Orçamento)
              │
spec-02 + spec-03 + spec-04
    └─► spec-05 (Relatórios e UI)
              │
spec-01 a spec-05
    └─► spec-06 (Backup e Restauração)

spec-05 + spec-06
    └─► spec-07 (Correção de Bugs de UX)
\`\`\`

> **Regra:** nunca inicie a implementação de uma spec sem que todas as suas dependências estejam com os Critérios de Aceite passando (testes verdes).

---

## 3. Ordem de Execução das Tasks

| Ordem | Task | Spec base | Depende de |
|---|---|---|---|
| 1 | [task-01](../tasks/task-01.md) | [spec-01](spec-01-database.md) | — |
| 2 | [task-02](../tasks/task-02.md) | [spec-02](spec-02-transactions.md) | task-01 |
| 3 | [task-03](../tasks/task-03.md) | [spec-03](spec-03-investments.md) | task-01 |
| 4 | [task-04](../tasks/task-04.md) | [spec-04](spec-04-budgeting.md) | task-01 |
| 5 | [task-05](../tasks/task-05.md) | [spec-05](spec-05-ui-reports.md) | task-02, task-03, task-04 |
| 6 | [task-06](../tasks/task-06.md) | [spec-06](spec-06-backup.md) | task-01 a task-05 |
| 7 | [task-07](../tasks/task-07.md) | [spec-07](spec-07-ux-bugfix.md) | task-05, task-06 |

---

## 4. Status das Specs

| Spec | Descrição | Status |
|---|---|---|
| [spec-01](spec-01-database.md) | Banco de dados, entidades e DAOs | `[x]` concluída |
| [spec-02](spec-02-transactions.md) | Transações e parcelamentos | `[x]` concluída |
| [spec-03](spec-03-investments.md) | Investimentos e aportes | `[x]` concluída |
| [spec-04](spec-04-budgeting.md) | Orçamento mensal | `[x]` concluída |
| [spec-05](spec-05-ui-reports.md) | Relatórios e UI Compose | `[x]` concluída |
| [spec-06](spec-06-backup.md) | Backup e restauração local | `[x]` concluída |
| [spec-07](spec-07-ux-bugfix.md) | Correção de bugs de usabilidade | `[ ]` pendente |

> Atualize este campo para `[/]` ao iniciar e `[x]` ao concluir (todos os testes verdes).

---

## 5. Glossário de Domínio

| Termo | Definição |
|---|---|
| **Transação** | Registro único de movimentação financeira — pode ser receita (`INCOME`) ou despesa (`EXPENSE`). |
| **Parcelamento** | Compra no cartão de crédito dividida em N parcelas mensais. Cada parcela é uma `TransactionEntity` separada, vinculadas pelo `installmentGroupId`. |
| **installmentGroupId** | UUID gerado no momento da criação de uma compra parcelada. Agrupa todas as parcelas de uma mesma compra. `null` para transações à vista. |
| **Aporte** | Entrada de dinheiro novo em um ativo de investimento. Registrado em `InvestmentContribution` e incrementa o `currentBalanceInCents` do ativo. |
| **Rendimento** | Diferença entre o saldo atual de um ativo e a soma total de aportes: rendimento = saldoAtual - soma(aportes). |
| **Horizonte** | Prazo estimado do investimento. Enum `InvestmentHorizon`: `SHORT` (curto), `MEDIUM` (médio), `LONG` (longo). |
| **Orçamento** | Teto de gastos planejado para uma categoria em um mês/ano específico (`MonthlyBudgetEntity`). Não afeta as transações reais. |
| **isEssential** | Flag booleana em `TransactionEntity` que classifica despesas como essenciais (aluguel, alimentação) ou não essenciais (lazer). Sempre `false` para receitas. |
| **Caixa Livre (C)** | Métrica calculada: C = Receitas - Despesas - Aportes. Representa o dinheiro disponível após honrar gastos e investimentos. |
| **SAF** | Storage Access Framework — API Android para permitir que o usuário escolha onde salvar/abrir arquivos sem permissão de armazenamento amplo. |
| **UDF** | Unidirectional Data Flow — padrão de arquitetura de UI onde o estado flui do ViewModel para a View e os eventos fluem da View para o ViewModel. |
