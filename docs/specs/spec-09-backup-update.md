# Spec 09: Atualização do Backup e Restauração Local (Payload v2)

## 1. Visão Geral
Esta especificação define a atualização do mecanismo de exportação e importação de dados em JSON (implementado originalmente na Spec 06) para a **versão 2 do schema do payload** (`version = 2`). A atualização é necessária para incluir as regras de receitas e despesas recorrentes (`recurring_transactions`, introduzidas na Spec 08) e preservar a chave de vínculo `recurringTransactionId` nas transações restauradas.

---

## 2. Estrutura do Payload de Backup v2

O payload exportado passa a ser da versão `2` e deve incluir o array `recurringTransactions` além das entidades já existentes:

```json
{
  "version": 2,
  "exportedAt": 1772614800000,
  "categories": [],
  "transactions": [],
  "investments": [],
  "investmentContributions": [],
  "monthlyBudgets": [],
  "recurringTransactions": []
}
```

### 2.1. Definição dos DTOs

```kotlin
@Serializable
data class RecurringTransactionBackupDto(
    val id: Long,
    val amountInCents: Long,
    val categoryId: Long,
    val type: String,
    val paymentMethod: String,
    val frequency: String,
    val startDate: Long,
    val endDate: Long? = null,
    val totalOccurrences: Int? = null,
    val generatedCount: Int = 0,
    val lastGeneratedDate: Long? = null,
    val isActive: Boolean = true,
    val isEssential: Boolean = false,
    val notes: String? = null
)

@Serializable
data class TransactionBackupDto(
    val id: Long,
    val amountInCents: Long,
    val timestamp: Long,
    val categoryId: Long,
    val type: String,
    val paymentMethod: String,
    val isEssential: Boolean = false,
    val installmentGroupId: String? = null,
    val installmentsCount: Int = 1,
    val currentInstallment: Int = 1,
    val recurringTransactionId: Long? = null,
    val notes: String? = null
)

@Serializable
data class BackupPayloadDto(
    val version: Int = 2,
    val exportedAt: Long,
    val categories: List<CategoryBackupDto>,
    val transactions: List<TransactionBackupDto>,
    val investments: List<InvestmentBackupDto>,
    val investmentContributions: List<InvestmentContributionBackupDto>,
    val monthlyBudgets: List<MonthlyBudgetBackupDto>,
    val recurringTransactions: List<RecurringTransactionBackupDto> = emptyList()
)
```

---

## 3. Regras de Negócio

### 3.1. Exportação (Versão 2)
- O arquivo gerado continuará utilizando a nomenclatura `financas_backup_YYYY-MM-DD.json`.
- O campo `version` no JSON gerado será sempre **`2`**.
- A exportação incluirá todas as 6 tabelas do banco de dados Room:
  1. `categories`
  2. `investments`
  3. `recurringTransactions`
  4. `transactions` (incluindo o campo `recurringTransactionId`)
  5. `investmentContributions`
  6. `monthlyBudgets`

### 3.2. Restauração (Importação) e Retrocompatibilidade
- O `ImportBackupUseCase` aceitará arquivos com `version <= 2`.
  - Se `version > 2`, a importação deve falhar lançando `UnsupportedBackupVersionException`.
- **Retrocompatibilidade com Versão 1:**
  - Se o usuário importar um arquivo JSON legado com `version == 1` (onde a propriedade `recurringTransactions` é ausente ou vazia e as transações não possuem `recurringTransactionId`), a importação **deve ser concluída com sucesso**, restaurando todas as categorias, transações, investimentos, aportes e orçamentos normais.
- **Ordem Transacional de Exclusão (Wipe):**
  - O wipe completo deve ser feito dentro de `AppDatabase.withTransaction` na ordem estrita de dependência de Foreign Keys (inversa da criação):
    1. `monthlyBudgets`
    2. `investmentContributions`
    3. `transactions`
    4. `recurringTransactions`
    5. `investments`
    6. `categories` (subcategorias primeiro, depois categorias raiz)
- **Ordem Transacional de Inserção:**
  - A reinserção deve respeitar a ordem de dependência de Foreign Keys:
    1. `categories` (raiz primeiro, depois subcategorias)
    2. `investments`
    3. `recurringTransactions` (depende de `categories`)
    4. `transactions` (depende de `categories` e opcionalmente `recurringTransactions`)
    5. `investmentContributions` (depende de `investments`)
    6. `monthlyBudgets` (depende de `categories`)

---

## 4. Critérios de Aceite (Testes Obrigatórios)

- [ ] **Teste 1 (Integração v2):** Ciclo completo v2 — exportar banco contendo dados em todas as 6 entidades (inclusive `recurringTransactions` e transações com `recurringTransactionId`), limpar o banco, importar o arquivo v2 gerado e verificar que 100% dos registros foram restaurados exatamente com os mesmos IDs e atributos.
- [ ] **Teste 2 (Retrocompatibilidade v1):** Importar arquivo de backup com `version = 1` (sem a propriedade `recurringTransactions`) deve restaurar perfeitamente as categorias, transações, investimentos, aportes e orçamentos sem lançar erros de desserialização ou Foreign Key.
- [ ] **Teste 3 (Versão Não Suportada):** Tentativa de importação com `version > 2` (ex: `version = 3`) deve retornar `Result.failure(UnsupportedBackupVersionException)`.
- [ ] **Teste 4 (Rollback em Falha):** Falha durante a inserção de `recurringTransactions` ou `transactions` deve acionar rollback total em `AppDatabase.withTransaction`, mantendo o estado prévio do banco 100% intacto.
- [ ] **Teste 5 (Formato do JSON Exportado):** Verificar se o JSON exportado possui `version = 2` e contém as chaves `categories`, `transactions`, `investments`, `investmentContributions`, `monthlyBudgets` e `recurringTransactions`.
