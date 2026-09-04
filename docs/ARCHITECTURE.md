# Arquitetura e Modelo de Dados

## Regras de Domínio
1. **Offline First Estrito:** Nenhuma dependência externa de rede (Firebase/APIs) deve ser adicionada.
2. **Normalização de Gastos:** Não existem tabelas separadas para "Essencial" e "Não Essencial". Essa distinção é feita via campo booleano (`isEssential`) na entidade de transação.
3. **Dívidas e Cartões:** Parcelamentos são registrados como registros de `Transaction` vinculados a um meio de pagamento `CREDIT_CARD`, contendo `installmentsCount` e `currentInstallment`.
4. **Valores Financeiros:** Para evitar problemas de arredondamento de ponto flutuante, os valores no banco de dados devem ser armazenados em centavos (`Long`) ou utilizando `BigDecimal` mapeado via `TypeConverter`.

## Esquema do Banco (Room)

### Enums
- `TransactionType`: `INCOME`, `EXPENSE`
- `CategoryType`: `INCOME`, `EXPENSE`, `INVESTMENT`
- `PaymentMethod`: `CASH`, `DEBIT`, `CREDIT_CARD`, `PIX`
- `InvestmentType`: `FIXED_INCOME`, `VARIABLE`

### Entidades

#### Category
- `id`: Long (PK, autoGenerate)
- `name`: String
- `type`: CategoryType
- `parentId`: Long? (FK opcional para subcategorias)

#### Transaction
- `id`: Long (PK, autoGenerate)
- `amountInCents`: Long
- `timestamp`: Long (Epoch millis)
- `categoryId`: Long (FK -> Category.id)
- `type`: TransactionType
- `paymentMethod`: PaymentMethod
- `isEssential`: Boolean
- `installmentsCount`: Int (padrão: 1)
- `currentInstallment`: Int (padrão: 1)
- `notes`: String?

#### Investment
- `id`: Long (PK, autoGenerate)
- `name`: String
- `type`: InvestmentType
- `horizon`: String (Curto, Médio, Longo)
- `currentBalanceInCents`: Long
