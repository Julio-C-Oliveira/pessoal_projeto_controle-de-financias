# Tarefa 02: Implementação da Camada de Domínio e Use Cases de Transação

## Instruções de Execução
1. Leia o arquivo `docs/specs/spec-02-transactions.md` e as regras de `.antigravity/rules.md`.
2. Crie o pacote `domain/usecase/transaction` com as seguintes classes:
   - `CreateTransactionUseCase`: implementando as regras de validação e o algoritmo de divisão de parcelas.
   - `DeleteTransactionUseCase`: garantindo remoção segura via repositório.
   - `GetTransactionsUseCase`: com filtragem por período de data.
3. Crie exceções customizadas de domínio em `domain/model/DomainExceptions.kt` para representar violações de regras (ex: `IncompatibleCategoryException`, `InvalidPaymentMethodException`).
4. Crie testes unitários puros com JUnit 4/5 e MockK (ou fake repository) na pasta `test/domain/usecase/transaction/`:
   - `CreateTransactionUseCaseTest`: cobrindo todos os critérios de aceite da Spec 02.
5. Execute `./gradlew testDebugUnitTest` e confirme que todos os testes da Spec 01 e Spec 02 estão verdes.
