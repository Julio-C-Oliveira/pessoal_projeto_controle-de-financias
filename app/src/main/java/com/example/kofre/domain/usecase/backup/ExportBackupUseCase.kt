package com.example.kofre.domain.usecase.backup

import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream

interface ExportBackupUseCase {
    suspend operator fun invoke(outputStream: OutputStream): Result<Unit>
}

class ExportBackupUseCaseImpl(
    private val repository: FinanceRepository,
    private val json: Json = Json { prettyPrint = true; encodeDefaults = true }
) : ExportBackupUseCase {

    override suspend fun invoke(outputStream: OutputStream): Result<Unit> {
        return Result.runCatching {
            val payload = repository.exportBackup()
            val jsonString = json.encodeToString(payload)
            outputStream.use { stream ->
                stream.write(jsonString.toByteArray(Charsets.UTF_8))
                stream.flush()
            }
        }
    }
}
