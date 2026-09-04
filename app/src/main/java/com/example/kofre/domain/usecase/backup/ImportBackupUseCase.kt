package com.example.kofre.domain.usecase.backup

import com.example.kofre.data.local.backup.BackupPayloadDto
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.serialization.json.Json
import java.io.InputStream

interface ImportBackupUseCase {
    suspend operator fun invoke(inputStream: InputStream): Result<Unit>
}

class ImportBackupUseCaseImpl(
    private val repository: FinanceRepository,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ImportBackupUseCase {

    companion object {
        const val CURRENT_SUPPORTED_VERSION = 2
    }

    override suspend fun invoke(inputStream: InputStream): Result<Unit> {
        return Result.runCatching {
            val jsonString = inputStream.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            }
            val payload = json.decodeFromString<BackupPayloadDto>(jsonString)
            if (payload.version > CURRENT_SUPPORTED_VERSION) {
                throw UnsupportedBackupVersionException("Versão de backup não suportada: ${payload.version}. Versão máxima suportada: $CURRENT_SUPPORTED_VERSION")
            }
            repository.importBackup(payload)
        }
    }
}
