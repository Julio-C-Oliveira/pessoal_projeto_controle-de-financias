# Spec 01: Modelo de Dados, Room e Camada de Persistência

## 1. Visão Geral
Esta especificação define a persistência local com Room para o aplicativo de finanças offline. O banco deve suportar categorias hierárquicas, transações com suporte a parcelas e ativos de investimentos, mantendo a integridade referencial estrita via Foreign Keys.

## 2. Tipos e Enums
- `TransactionType`: `INCOME`, `EXPENSE`
- `CategoryType`: `INCOME`, `EXPENSE`, `INVESTMENT`
- `PaymentMethod`: `CASH`, `DEBIT`, `CREDIT_CARD`, `PIX`
- `InvestmentType`: `FIXED_INCOME`, `VARIABLE`
- `InvestmentHorizon`: `SHORT`, `MEDIUM`, `LONG` — mapeado via `TypeConverter` para `String` no banco.

## 3. Esquema das Entidades

### 3.1. CategoryEntity (`categories`)
- `id`: Long (PK, autoGenerate = true)
- `name`: String (Not Null)
- `type`: String (Mapeado de `CategoryType`)
- `parentId`: Long? (FK opcional para `categories.id` com `onDelete = RESTRICT`)

### 3.2. TransactionEntity (`transactions`)
- `id`: Long (PK, autoGenerate = true)
- `amountInCents`: Long (Not Null, deve ser > 0)
- `timestamp`: Long (Epoch millis, Not Null)
- `categoryId`: Long (FK para `categories.id` com `onDelete = RESTRICT`)
- `type`: String (Mapeado de `TransactionType`)
- `paymentMethod`: String (Mapeado de `PaymentMethod`)
- `isEssential`: Boolean (Default false)
- `installmentGroupId`: String? (UUID v4 gerado no momento da criação; `null` quando `installmentsCount == 1`)
- `installmentsCount`: Int (Default 1, deve ser >= 1)
- `currentInstallment`: Int (Default 1, deve ser >= 1)
- `notes`: String? (Opcional)

### 3.3. InvestmentEntity (`investments`)
- `id`: Long (PK, autoGenerate = true)
- `name`: String (Not Null)
- `type`: String (Mapeado de `InvestmentType`)
- `horizon`: String (Mapeado de `InvestmentHorizon` via TypeConverter, valores: `"SHORT"`, `"MEDIUM"`, `"LONG"`)
- `currentBalanceInCents`: Long (Default 0, deve ser >= 0)

## 4. Contratos de DAO

### CategoryDao
- `fun getAllCategories(): Flow<List<CategoryEntity>>`
- `fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>`
- `suspend fun insertCategory(category: CategoryEntity): Long`
- `suspend fun deleteCategory(category: CategoryEntity)`

### TransactionDao
- `fun getAllTransactions(): Flow<List<TransactionEntity>>`
- `fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>`
- `fun getTransactionsByGroupId(groupId: String): Flow<List<TransactionEntity>>`
- `suspend fun insertTransaction(transaction: TransactionEntity): Long`
- `suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>`
- `suspend fun deleteTransaction(transaction: TransactionEntity)`
- `suspend fun deleteTransactionsByGroupId(groupId: String)`

### InvestmentDao
- `fun getAllInvestments(): Flow<List<InvestmentEntity>>`
- `suspend fun insertInvestment(investment: InvestmentEntity): Long`
- `suspend fun updateBalance(id: Long, newBalanceInCents: Long)`

## 5. Contrato do Repositório (`FinanceRepository`)
Deve expor modelos de domínio imutáveis (sem anotações do Room):
- `Transaction`: Contendo ID, amountInCents, data formatada/timestamp, categoria associada resolvida, tipo, método de pagamento e flag essencial.
- `Category`: Contendo ID, nome, tipo e lista de subcategorias filhas.
- `Investment`: Contendo ID, nome, tipo, horizonte e saldo atual.

## 6. Critérios de Aceite (Testes Unitários Obrigatórios)
- [ ] Criar testes instrumentados/unitários usando `Room.inMemoryDatabaseBuilder`.
- [ ] **Teste 1:** Inserir categoria e validar recuperação via `Flow`.
- [ ] **Teste 2:** Falhar com `SQLiteConstraintException` ao inserir transação com `categoryId` inexistente.
- [ ] **Teste 3:** Impedir a deleção de uma categoria que possua transações vinculadas (`RESTRICT`).
- [ ] **Teste 4:** Atualizar o saldo de um investimento e verificar a emissão do novo valor no `Flow`.
- [ ] **Teste 5:** Inserir 3 parcelas com o mesmo `installmentGroupId` e validar que `getTransactionsByGroupId` retorna exatamente as 3 registros.
