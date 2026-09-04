# Spec 06: Backup e Restauração de Dados Local (JSON)

## 1. Visão Geral
Esta especificação define o mecanismo de exportação e importação de dados para persistência externa em arquivo `.json`. Por ser uma aplicação estritamente offline, o usuário deve ser capaz de salvar o estado completo do banco de dados no armazenamento local do dispositivo (via Storage Access Framework - SAF) e restaurá-lo em caso de migração de aparelho ou reinstalação.

## 2. Estrutura do Payload de Backup

O payload exportado deve conter metadados de controle de versão e os arrays de dados de todas as entidades do banco:

```json
{
  "version": 1,
  "exportedAt": 1772614800000,
  "categories": [],
  "transactions": [],
  "investments": [],
  "investmentContributions": [],
  "monthlyBudgets": []
}
```

## 3. Regras de Negócio

### 3.1. Exportação
- O arquivo gerado deve ter o nome no formato: `financas_backup_YYYY-MM-DD.json`.
- A exportação inclui todas as entidades na ordem: `categories`, `transactions`, `investments`, `investmentContributions`, `monthlyBudgets`.
- O campo `version` deve refletir a versão atual do esquema de backup (inicialmente `1`). Deve ser incrementado sempre que a estrutura do payload mudar de forma incompatível.

### 3.2. Restauração (Importação)
- Antes de qualquer insert, o `ImportBackupUseCase` deve validar o campo `version`:
  - Se `version > versão suportada pelo app`, a importação deve falhar com `UnsupportedBackupVersionException`.
  - Por ora, versão `1` é a única suportada.
- A restauração deve ser **totalmente transacional**: toda a operação de wipe + insert deve estar dentro de `AppDatabase.withTransaction`. Se qualquer etapa falhar, nenhuma alteração é persistida.
- A ordem de inserção deve respeitar as Foreign Keys:
  1. `categories`
  2. `investments`
  3. `transactions` (depende de `categories`)
  4. `investmentContributions` (depende de `investments`)
  5. `monthlyBudgets` (depende de `categories`)
- Antes de inserir, todos os dados existentes devem ser deletados (wipe completo) na ordem inversa das FKs.

## 4. Contratos de Use Cases (Camada de Domínio)

```kotlin
interface ExportBackupUseCase {
    // Lê todos os dados do repositório, serializa e escreve no OutputStream fornecido pelo SAF.
    suspend operator fun invoke(outputStream: OutputStream): Result<Unit>
}

interface ImportBackupUseCase {
    // Lê e desserializa o JSON do InputStream, valida a versão e repassa ao repositório.
    suspend operator fun invoke(inputStream: InputStream): Result<Unit>
}
```

## 5. Critérios de Aceite (Testes Obrigatórios)

- [ ] **Teste 1 (Integração):** Ciclo completo — exportar banco com dados, limpar, importar e verificar que todos os registros foram restaurados com os mesmos IDs e valores.
- [ ] **Teste 2:** `ImportBackupUseCase` deve retornar `Result.failure(UnsupportedBackupVersionException)` ao receber payload com `version > 1`.
- [ ] **Teste 3:** Falha durante a inserção de `transactions` deve fazer rollback completo, deixando o banco intacto.
- [ ] **Teste 4:** A restauração não deve lançar `SQLiteConstraintException` para um payload válido.
- [ ] **Teste 5:** O JSON exportado deve conter os campos: `version`, `exportedAt`, `categories`, `transactions`, `investments`, `investmentContributions` e `monthlyBudgets`.

