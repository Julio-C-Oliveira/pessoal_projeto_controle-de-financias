# Arquitetura e Modelo de Dados

## Regras de Domínio
1. **Offline First Estrito:** Nenhuma dependência externa de rede (Firebase/APIs) deve ser adicionada.
2. **Normalização de Gastos:** Não existem tabelas separadas para "Essencial" e "Não Essencial". Essa distinção é feita via campo booleano (`isEssential`) na entidade de transação.
3. **Dívidas e Cartões:** Parcelamentos são registrados como registros de `Transaction` vinculados a um meio de pagamento `CREDIT_CARD`, contendo `installmentsCount` e `currentInstallment`.
4. **Valores Financeiros:** Para evitar problemas de arredondamento de ponto flutuante, os valores no banco de dados devem ser armazenados em centavos (`Long`) ou utilizando `BigDecimal` mapeado via `TypeConverter`.

## Esquema do Banco (Room)

### Enums
- `TransactionType`: `INCOME`, `EXPENSE`
- `CategoryType`: `INCOME`, `EXPENSE`, `INVESTMENT` ¹
- `PaymentMethod`: `CASH`, `DEBIT`, `CREDIT_CARD`, `PIX`
- `InvestmentType`: `FIXED_INCOME`, `VARIABLE`
- `InvestmentHorizon`: `SHORT` (Curto prazo), `MEDIUM` (Médio prazo), `LONG` (Longo prazo)

> ¹ `CategoryType.INVESTMENT` é reservado para categorizar aportes de investimento nos relatórios (Spec 05), distinguindo-os de despesas comuns no cálculo de Caixa Livre ($C = R - D - I$). Transações do tipo `INCOME` e `EXPENSE` nunca usam essa categoria.

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
- `installmentGroupId`: String? (UUID, preenchido apenas quando `installmentsCount > 1`; identifica o grupo de parcelas)
- `installmentsCount`: Int (padrão: 1)
- `currentInstallment`: Int (padrão: 1)
- `notes`: String?

#### Investment
- `id`: Long (PK, autoGenerate)
- `name`: String
- `type`: InvestmentType
- `horizon`: InvestmentHorizon
- `currentBalanceInCents`: Long
