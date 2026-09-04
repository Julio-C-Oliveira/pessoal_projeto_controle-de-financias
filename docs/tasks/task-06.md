# Tarefa 06: Implementação do Módulo de Backup e Restauração Local

## Instruções de Execução
1. Consulte `docs/specs/spec-06-backup.md` e as regras de `.antigravity/rules.md`.
2. Adicione a dependência da biblioteca oficial `org.jetbrains.kotlinx:kotlinx-serialization-json` no `build.gradle.kts` e configure o plugin `kotlinx.serialization`.
3. Crie o pacote `data/local/backup/` com:
   - Os DTOs serializáveis (`BackupPayloadDto`, `CategoryBackupDto`, etc.).
   - Mappers para converter entre as `Entity` do Room e os DTOs serializáveis.
4. Implemente as rotinas de importação/exportação no `FinanceRepositoryImpl`:
   - Utilize `AppDatabase.withTransaction` para envelopar o processo de deleção e reinserção dos dados.
   - Respeite a ordem estrita de inserção para não disparar falhas de foreign key.
5. Crie os use cases no pacote `domain/usecase/backup/`:
   - `ExportBackupUseCase`: lê do repositório e escreve em uma `OutputStream`.
   - `ImportBackupUseCase`: lê da `InputStream`, valida e repassa ao repositório.
6. Adicione uma seção simples de "Backup / Restauração" nas configurações da UI usando as APIs do Android Compose (`rememberLauncherForActivityResult` com `ActivityResultContracts.CreateDocument` e `OpenDocument`).
7. Escreva testes instrumentados ou unitários com banco in-memory em `BackupIntegrationTest.kt` validando todo o ciclo de exportação e restauração.
8. Execute `./gradlew testDebugUnitTest` e `./gradlew assembleDebug` para certificar que o build permanece sem avisos ou erros.
