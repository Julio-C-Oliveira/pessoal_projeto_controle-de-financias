# Tarefa 07: Correção de Bugs de Usabilidade (UX Bugfix)

## Instruções de Execução

1. Consulte `docs/specs/spec-07-ux-bugfix.md` e as regras de `.antigravity/rules.md` antes de iniciar.
2. Siga a ordem das etapas abaixo, pois há dependências entre camadas.

---

### Etapa 1 — BUG-02: Corrigir dropdown de categoria em `TransactionsScreen`

1. Abra `app/src/main/java/com/example/kofre/ui/screens/transactions/TransactionsScreen.kt`.
2. No composable `AddTransactionDialog`, localize todas as chamadas `.menuAnchor()` dentro dos `ExposedDropdownMenuBox`.
3. Substitua cada `.menuAnchor()` por `.menuAnchor(MenuAnchorType.PrimaryNotEditable)`.
4. Adicione o import necessário: `import androidx.compose.material3.MenuAnchorType`.
5. Valide o AC-02: ao tocar no campo de categoria, o menu deve abrir com as opções filtradas corretamente.

---

### Etapa 2 — BUG-01: Corrigir layout de "Forma de Pagamento" em `TransactionsScreen`

1. No mesmo arquivo `TransactionsScreen.kt`, localize a seção `// Payment Method` no `AddTransactionDialog`.
2. Substitua o `Row` único que itera sobre `PaymentMethod.values()` por um layout em grade `2×2`:
   - Primeiro `Row`: `CASH` e `DEBIT`.
   - Segundo `Row`: `CREDIT_CARD` e `PIX`.
3. Substitua `method.name` por um `when` que retorne o label PT-BR conforme a tabela da spec:
   - `CASH` → `"Dinheiro"`, `DEBIT` → `"Débito"`, `CREDIT_CARD` → `"Cartão de Crédito"`, `PIX` → `"Pix"`.
4. Valide o AC-01: os 4 métodos devem ser exibidos sem sobreposição.

---

### Etapa 3 — BUG-02: Corrigir dropdown de categoria em `BudgetScreen`

1. Abra `app/src/main/java/com/example/kofre/ui/screens/budget/BudgetScreen.kt`.
2. No composable `SetBudgetDialog`, substitua `.menuAnchor()` por `.menuAnchor(MenuAnchorType.PrimaryNotEditable)`.
3. Adicione o import: `import androidx.compose.material3.MenuAnchorType`.
4. Adicione um guard antes do `ExposedDropdownMenuBox`:
   ```kotlin
   if (categories.isEmpty()) {
       Text(
           text = "Nenhuma categoria de despesa disponível.",
           style = MaterialTheme.typography.bodyMedium,
           color = MaterialTheme.colorScheme.onSurfaceVariant
       )
   } else {
       // ExposedDropdownMenuBox existente aqui
   }
   ```
5. Valide os AC-03 e AC-04.

---

### Etapa 4 — BUG-03: Adicionar suporte a exclusão de investimento na camada de dados

1. Abra `app/src/main/java/com/example/kofre/data/local/dao/InvestmentDao.kt`.
2. Verifique se já existe um método de deleção por ID. Se não existir, adicione:
   ```kotlin
   @Query("DELETE FROM investments WHERE id = :id")
   suspend fun deleteById(id: Long)
   ```
3. Abra `app/src/main/java/com/example/kofre/domain/repository/FinanceRepository.kt`.
4. Adicione a assinatura: `suspend fun deleteInvestment(id: Long)`.
5. Abra `app/src/main/java/com/example/kofre/data/repository/FinanceRepositoryImpl.kt`.
6. Implemente o método delegando ao DAO:
   ```kotlin
   override suspend fun deleteInvestment(id: Long) {
       investmentDao.deleteById(id)
   }
   ```

---

### Etapa 5 — BUG-03: Criar `DeleteInvestmentUseCase`

1. Crie o arquivo `app/src/main/java/com/example/kofre/domain/usecase/investment/DeleteInvestmentUseCase.kt`:
   ```kotlin
   interface DeleteInvestmentUseCase {
       suspend operator fun invoke(investmentId: Long): Result<Unit>
   }
   ```
2. Crie o arquivo `app/src/main/java/com/example/kofre/domain/usecase/investment/DeleteInvestmentUseCaseImpl.kt`:
   ```kotlin
   class DeleteInvestmentUseCaseImpl(
       private val repository: FinanceRepository
   ) : DeleteInvestmentUseCase {
       override suspend operator fun invoke(investmentId: Long): Result<Unit> {
           return runCatching { repository.deleteInvestment(investmentId) }
       }
   }
   ```

---

### Etapa 6 — BUG-03: Atualizar `InvestmentsViewModel`

1. Abra `app/src/main/java/com/example/kofre/ui/screens/investments/InvestmentsViewModel.kt`.
2. Adicione o parâmetro `deleteInvestmentUseCase: DeleteInvestmentUseCase` no construtor da classe.
3. Adicione o método:
   ```kotlin
   suspend fun deleteInvestment(investmentId: Long): Boolean {
       _errorMessage.value = null
       val result = deleteInvestmentUseCase(investmentId)
       return if (result.isSuccess) {
           true
       } else {
           _errorMessage.value = result.exceptionOrNull()?.message ?: "Erro ao excluir investimento."
           false
       }
   }
   ```

---

### Etapa 7 — BUG-03: Atualizar UI em `InvestmentsScreen`

1. Abra `app/src/main/java/com/example/kofre/ui/screens/investments/InvestmentsScreen.kt`.
2. No composable `InvestmentsScreen`, adicione o estado: `var assetToDelete by remember { mutableStateOf<InvestmentDetail?>(null) }`.
3. No composable `InvestmentAssetCard`, adicione o parâmetro `onDeleteClick: () -> Unit` e insira um `IconButton` com `Icons.Default.Delete` ao lado dos botões existentes:
   ```kotlin
   IconButton(onClick = onDeleteClick) {
       Icon(Icons.Default.Delete, contentDescription = "Excluir Ativo", tint = MaterialTheme.colorScheme.error)
   }
   ```
4. Atualize a chamada de `InvestmentAssetCard` no `LazyColumn` para passar o callback:
   ```kotlin
   onDeleteClick = { assetToDelete = asset }
   ```
5. Crie o composable `DeleteInvestmentDialog`:
   ```kotlin
   @Composable
   fun DeleteInvestmentDialog(
       assetName: String,
       onDismiss: () -> Unit,
       onConfirm: () -> Unit
   ) {
       AlertDialog(
           onDismissRequest = onDismiss,
           title = { Text("Excluir Ativo") },
           text = { Text("Deseja realmente excluir o ativo \"$assetName\"? Esta ação também removerá todos os aportes associados.") },
           confirmButton = {
               Button(
                   onClick = onConfirm,
                   colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
               ) { Text("Excluir") }
           },
           dismissButton = {
               TextButton(onClick = onDismiss) { Text("Cancelar") }
           }
       )
   }
   ```
6. Conecte o dialog ao estado `assetToDelete` no `InvestmentsScreen`, similar ao padrão já usado com `selectedAssetForContribution`.

---

### Etapa 8 — Atualizar `AppNavigation`

1. Abra `app/src/main/java/com/example/kofre/ui/navigation/AppNavigation.kt`.
2. Instancie o novo use case:
   ```kotlin
   val deleteInvestmentUseCase = DeleteInvestmentUseCaseImpl(repository)
   ```
3. Passe-o para o `InvestmentsViewModel`:
   ```kotlin
   val investmentsViewModel = InvestmentsViewModel(
       repository,
       getInvestmentsSummaryUseCase,
       createInvestmentUseCase,
       addContributionUseCase,
       updateInvestmentBalanceUseCase,
       deleteInvestmentUseCase  // <-- novo
   )
   ```

---

### Etapa 9 — Verificação Final

1. Execute `./gradlew assembleDebug` e certifique-se de que o build conclui **sem erros ou warnings de compilação**.
2. Instale o APK no dispositivo/emulador e valide manualmente cada critério de aceite (AC-01 a AC-09) listados em `docs/specs/spec-07-ux-bugfix.md`.
3. Atualize o status da spec em `docs/specs/spec-00-overview.md`:
   - Adicione a linha `spec-07` na tabela de status com `[x] concluída`.
