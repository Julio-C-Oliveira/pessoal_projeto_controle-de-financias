# Spec 07: Correção de Bugs de Usabilidade (UX Bugfix)

## 1. Visão Geral

Esta especificação cobre a correção de três bugs de usabilidade identificados no teste manual do aplicativo. Todos os bugs estão **isolados na camada de UI (Jetpack Compose)** e na camada de domínio do módulo de investimentos. Nenhuma alteração de esquema de banco de dados (Room migrations) é necessária.

### Bugs cobertos

| # | Tela | Descrição | Camadas afetadas |
|---|------|-----------|-----------------|
| BUG-01 | Nova Transação | Os 4 métodos de pagamento se sobrepõem visualmente em um único `Row` | UI |
| BUG-02 | Nova Transação e Orçamento | O dropdown "Selecionar categoria" não abre ao ser tocado | UI |
| BUG-03 | Investimentos | Não existe opção para remover um ativo de investimento | Domain + UI |

---

## 2. Diagnóstico e Causa Raiz

### BUG-01 — Sobreposição de Formas de Pagamento

**Arquivo:** `TransactionsScreen.kt` → composable `AddTransactionDialog`

**Causa:** O layout da seção "Forma de Pagamento" usa um único `Row` horizontal para iterar sobre todos os valores de `PaymentMethod` (`CASH`, `DEBIT`, `CREDIT_CARD`, `PIX`). O enum `CREDIT_CARD` tem nome longo que ultrapassa a largura disponível, causando quebra de linha e sobreposição visual com o elemento seguinte.

**Fix:** Substituir o `Row` único por um layout em grade `2×2` usando dois `Row`s com dois itens cada. Substituir `method.name` por labels amigáveis em português (`Dinheiro`, `Débito`, `Cartão de Crédito`, `Pix`).

---

### BUG-02 — Dropdown de Categoria não abre

**Arquivos:** `TransactionsScreen.kt` e `BudgetScreen.kt`

**Causa:** Ambas as telas utilizam `ExposedDropdownMenuBox` com `.menuAnchor()` sem argumentos. Na versão de Material3 do projeto, essa sobrecarga sem parâmetros está marcada como **deprecated** e pode não registrar o clique corretamente, impedindo a abertura do menu.

**Fix:**
1. Substituir `.menuAnchor()` por `.menuAnchor(MenuAnchorType.PrimaryNotEditable)` em todos os `ExposedDropdownMenuBox` das duas telas.
2. No `SetBudgetDialog` (BudgetScreen), adicionar um guard: se `categories` estiver vazia, exibir `Text("Nenhuma categoria de despesa disponível.")` no lugar do dropdown, evitando um dropdown silenciosamente vazio.

---

### BUG-03 — Ausência de remoção de ativo de investimento

**Arquivos:** `InvestmentsScreen.kt`, `InvestmentsViewModel.kt`, `FinanceRepository`, `FinanceRepositoryImpl`, `InvestmentDao`

**Causa:** O `InvestmentAssetCard` expõe apenas os callbacks `onAddContribution` e `onUpdateBalance`. Não existe botão de exclusão no card, e toda a pilha de suporte (use case, método de repositório) também está ausente.

**Fix (em camadas, de baixo para cima):**
1. Verificar/adicionar `@Query("DELETE FROM investments WHERE id = :id")` no `InvestmentDao`.
2. Adicionar `suspend fun deleteInvestment(id: Long)` em `FinanceRepository` e `FinanceRepositoryImpl`.
3. Criar `DeleteInvestmentUseCase` (interface) e `DeleteInvestmentUseCaseImpl`.
4. Injetar o use case no `InvestmentsViewModel` e adicionar `suspend fun deleteInvestment(id: Long): Boolean`.
5. Adicionar ícone `Delete` no `InvestmentAssetCard` e um `DeleteInvestmentDialog` de confirmação.
6. Atualizar `AppNavigation` para instanciar e injetar `DeleteInvestmentUseCaseImpl`.

> **Regra de exclusão em cascata:** Ao deletar um investimento, todos os registros de `InvestmentContribution` associados **devem ser excluídos junto**. Verificar que a `ForeignKey` em `InvestmentContributionEntity` já define `onDelete = ForeignKey.CASCADE`. Se não estiver, adicionar via Room migration.

---

## 3. Contratos de Domínio

### 3.1. DeleteInvestmentUseCase (novo)

```kotlin
interface DeleteInvestmentUseCase {
    suspend operator fun invoke(investmentId: Long): Result<Unit>
}
```

### 3.2. FinanceRepository (adição)

```kotlin
suspend fun deleteInvestment(id: Long)
```

---

## 4. Regras de UX

- O `DeleteInvestmentDialog` deve exibir o **nome do ativo** no texto de confirmação para evitar exclusão acidental.
- Os labels de `PaymentMethod` em PT-BR devem seguir a tabela:

| Enum | Label PT-BR |
|------|------------|
| `CASH` | Dinheiro |
| `DEBIT` | Débito |
| `CREDIT_CARD` | Cartão de Crédito |
| `PIX` | Pix |

---

## 5. Critérios de Aceite

- [ ] **AC-01:** No dialog "Nova Transação", os 4 métodos de pagamento são exibidos sem sobreposição, em layout 2×2, com labels em português.
- [ ] **AC-02:** No dialog "Nova Transação", ao tocar no campo "Selecione a categoria", o dropdown abre e lista as categorias filtradas pelo tipo selecionado (Despesa/Receita).
- [ ] **AC-03:** No dialog de Orçamento, ao tocar no campo de categoria, o dropdown abre e lista as categorias de despesa disponíveis.
- [ ] **AC-04:** Se não houver categorias de despesa cadastradas, o dialog de Orçamento exibe a mensagem "Nenhuma categoria de despesa disponível." em vez de um dropdown vazio.
- [ ] **AC-05:** Na tela de Investimentos, cada card de ativo exibe um ícone de exclusão (lixeira).
- [ ] **AC-06:** Ao tocar na lixeira, um dialog de confirmação é exibido com o nome do ativo.
- [ ] **AC-07:** Ao confirmar a exclusão, o ativo some da lista e o patrimônio total é recalculado.
- [ ] **AC-08:** Ao cancelar, nenhuma alteração é persistida.
- [ ] **AC-09:** O build `./gradlew assembleDebug` conclui sem erros após as alterações.
