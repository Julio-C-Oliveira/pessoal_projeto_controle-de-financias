package com.example.kofre.ui.screens.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kofre.domain.usecase.backup.ExportBackupUseCase
import com.example.kofre.domain.usecase.backup.ImportBackupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupUiState(
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val isError: Boolean = false
)

class BackupViewModel(
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun exportBackup(outputStream: OutputStream) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, userMessage = null)
            val result = exportBackupUseCase(outputStream)
            result.onSuccess {
                _uiState.value = BackupUiState(
                    isLoading = false,
                    userMessage = "Backup exportado com sucesso!",
                    isError = false
                )
            }.onFailure { error ->
                _uiState.value = BackupUiState(
                    isLoading = false,
                    userMessage = "Falha ao exportar backup: ${error.localizedMessage}",
                    isError = true
                )
            }
        }
    }

    fun importBackup(inputStream: InputStream) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, userMessage = null)
            val result = importBackupUseCase(inputStream)
            result.onSuccess {
                _uiState.value = BackupUiState(
                    isLoading = false,
                    userMessage = "Backup restaurado com sucesso!",
                    isError = false
                )
            }.onFailure { error ->
                _uiState.value = BackupUiState(
                    isLoading = false,
                    userMessage = "Falha ao restaurar backup: ${error.localizedMessage}",
                    isError = true
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(userMessage = null)
    }

    fun getSuggestedFileName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return "financas_backup_${dateFormat.format(Date())}.json"
    }
}
