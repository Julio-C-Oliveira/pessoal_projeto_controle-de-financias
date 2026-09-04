package com.example.kofre.ui.screens.investments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.domain.model.InvestmentDetail
import com.example.kofre.ui.util.Formatters
import kotlinx.coroutines.launch

@Composable
fun InvestmentsScreen(
    viewModel: InvestmentsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showAddAssetDialog by remember { mutableStateOf(false) }
    var selectedAssetForContribution by remember { mutableStateOf<InvestmentDetail?>(null) }
    var selectedAssetForBalanceUpdate by remember { mutableStateOf<InvestmentDetail?>(null) }
    var assetToDelete by remember { mutableStateOf<InvestmentDetail?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddAssetDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Ativo")
            }
        }
    ) { innerPadding ->
        if (state.isLoading || state.summary == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val summary = state.summary!!

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Carteira de Investimentos",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Patrimônio Total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = Formatters.formatCentsToCurrency(summary.totalInvestedInCents),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Aportado", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    Formatters.formatCentsToCurrency(summary.totalContributionsInCents),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Rendimento Total", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    Formatters.formatCentsToCurrency(summary.totalYieldInCents),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (summary.totalYieldInCents >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Ativos Cadastrados",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (summary.investments.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Nenhum ativo de investimento cadastrado.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(summary.investments) { asset ->
                    InvestmentAssetCard(
                        asset = asset,
                        onAddContribution = { selectedAssetForContribution = asset },
                        onUpdateBalance = { selectedAssetForBalanceUpdate = asset },
                        onDeleteClick = { assetToDelete = asset }
                    )
                }
            }
        }
    }

    if (showAddAssetDialog) {
        AddInvestmentDialog(
            onDismiss = { showAddAssetDialog = false },
            onConfirm = { name, type, horizon, initialBalance ->
                coroutineScope.launch {
                    val success = viewModel.createInvestment(name, type, horizon, initialBalance)
                    if (success) showAddAssetDialog = false
                }
            }
        )
    }

    selectedAssetForContribution?.let { asset ->
        AddContributionDialog(
            assetName = asset.name,
            onDismiss = { selectedAssetForContribution = null },
            onConfirm = { amountInCents, notes ->
                coroutineScope.launch {
                    val success = viewModel.addContribution(asset.id, amountInCents, notes)
                    if (success) selectedAssetForContribution = null
                }
            }
        )
    }

    selectedAssetForBalanceUpdate?.let { asset ->
        UpdateBalanceDialog(
            assetName = asset.name,
            currentBalanceInCents = asset.currentBalanceInCents,
            onDismiss = { selectedAssetForBalanceUpdate = null },
            onConfirm = { newBalanceInCents ->
                coroutineScope.launch {
                    val success = viewModel.updateBalance(asset.id, newBalanceInCents)
                    if (success) selectedAssetForBalanceUpdate = null
                }
            }
        )
    }

    assetToDelete?.let { asset ->
        DeleteInvestmentDialog(
            assetName = asset.name,
            onDismiss = { assetToDelete = null },
            onConfirm = {
                coroutineScope.launch {
                    viewModel.deleteInvestment(asset.id)
                    assetToDelete = null
                }
            }
        )
    }
}

@Composable
fun InvestmentAssetCard(
    asset: InvestmentDetail,
    onAddContribution: () -> Unit,
    onUpdateBalance: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val yieldInCents = asset.currentBalanceInCents - asset.totalAportadoInCents

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = asset.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${asset.type.name} • Horizon ${asset.horizon.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = onAddContribution) {
                        Icon(Icons.Default.Payments, contentDescription = "Aporte", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onUpdateBalance) {
                        Icon(Icons.Default.Edit, contentDescription = "Atualizar Saldo", tint = MaterialTheme.colorScheme.secondary)
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir Ativo", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Saldo Atual", style = MaterialTheme.typography.bodySmall)
                    Text(
                        Formatters.formatCentsToCurrency(asset.currentBalanceInCents),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Aportado", style = MaterialTheme.typography.bodySmall)
                    Text(
                        Formatters.formatCentsToCurrency(asset.totalAportadoInCents),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Rendimento", style = MaterialTheme.typography.bodySmall)
                    Text(
                        Formatters.formatCentsToCurrency(yieldInCents),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (yieldInCents >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvestmentDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: InvestmentType, horizon: InvestmentHorizon, initialBalance: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(InvestmentType.FIXED_INCOME) }
    var selectedHorizon by remember { mutableStateOf(InvestmentHorizon.MEDIUM) }
    var initialBalanceText by remember { mutableStateOf("0") }

    var expandedType by remember { mutableStateOf(false) }
    var expandedHorizon by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Ativo de Investimento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Ativo (e.g. Tesouro Selic)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = !expandedType }
                ) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Investimento") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        InvestmentType.values().forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.name) },
                                onClick = {
                                    selectedType = t
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                // Horizon Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedHorizon,
                    onExpandedChange = { expandedHorizon = !expandedHorizon }
                ) {
                    OutlinedTextField(
                        value = selectedHorizon.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Horizonte") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedHorizon) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedHorizon,
                        onDismissRequest = { expandedHorizon = false }
                    ) {
                        InvestmentHorizon.values().forEach { h ->
                            DropdownMenuItem(
                                text = { Text(h.name) },
                                onClick = {
                                    selectedHorizon = h
                                    expandedHorizon = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = initialBalanceText,
                    onValueChange = { initialBalanceText = it },
                    label = { Text("Saldo Inicial (R$)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val initialBalance = ((initialBalanceText.replace(",", ".").toDoubleOrNull() ?: 0.0) * 100).toLong()
                    if (name.isNotBlank()) {
                        onConfirm(name, selectedType, selectedHorizon, initialBalance)
                    }
                }
            ) {
                Text("Criar Ativo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun AddContributionDialog(
    assetName: String,
    onDismiss: () -> Unit,
    onConfirm: (amountInCents: Long, notes: String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Aporte: $assetName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor do Aporte (R$)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Observações (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountInCents = ((amountText.replace(",", ".").toDoubleOrNull() ?: 0.0) * 100).toLong()
                    if (amountInCents > 0) {
                        onConfirm(amountInCents, notesText.ifBlank { null })
                    }
                }
            ) {
                Text("Aportar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun UpdateBalanceDialog(
    assetName: String,
    currentBalanceInCents: Long,
    onDismiss: () -> Unit,
    onConfirm: (newBalanceInCents: Long) -> Unit
) {
    var balanceText by remember { mutableStateOf((currentBalanceInCents / 100.0).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atualizar Saldo: $assetName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Novo Saldo Atual (R$)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newBalance = ((balanceText.replace(",", ".").toDoubleOrNull() ?: 0.0) * 100).toLong()
                    onConfirm(newBalance)
                }
            ) {
                Text("Atualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DeleteInvestmentDialog(
    assetName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir Ativo") },
        text = { Text("Deseja realmente excluir o ativo \"$assetName\"? Esta ação também removerá todos os aportes associados.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Excluir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
