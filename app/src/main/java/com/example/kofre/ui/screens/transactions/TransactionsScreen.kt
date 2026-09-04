package com.example.kofre.ui.screens.transactions

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.RadioButton
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
import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.model.Transaction
import com.example.kofre.ui.util.Formatters
import kotlinx.coroutines.launch

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Transação")
            }
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Extrato de Transações",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (state.transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Nenhuma transação encontrada.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.transactions) { tx ->
                        TransactionDetailCard(
                            transaction = tx,
                            onDeleteClick = { transactionToDelete = tx }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            categories = state.categories,
            onDismiss = { showAddDialog = false },
            onConfirm = { amountInCents, catId, type, method, isEssential, installments, notes ->
                coroutineScope.launch {
                    val success = viewModel.createTransaction(
                        amountInCents = amountInCents,
                        categoryId = catId,
                        type = type,
                        paymentMethod = method,
                        isEssential = isEssential,
                        installmentsCount = installments,
                        notes = notes
                    )
                    if (success) {
                        showAddDialog = false
                    }
                }
            }
        )
    }

    transactionToDelete?.let { tx ->
        DeleteTransactionDialog(
            transaction = tx,
            onDismiss = { transactionToDelete = null },
            onConfirm = { deleteGroup ->
                coroutineScope.launch {
                    viewModel.deleteTransaction(tx.id, deleteGroup)
                    transactionToDelete = null
                }
            }
        )
    }
}

@Composable
fun TransactionDetailCard(
    transaction: Transaction,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (transaction.type == TransactionType.INCOME)
                        Color(0xFF2E7D32).copy(alpha = 0.15f)
                    else
                        Color(0xFFC62828).copy(alpha = 0.15f)
                ) {
                    Icon(
                        imageVector = if (transaction.type == TransactionType.INCOME) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (transaction.type == TransactionType.INCOME) Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.category?.name ?: "Categoria ${transaction.categoryId}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${Formatters.formatDate(transaction.timestamp)} • ${transaction.paymentMethod.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (transaction.installmentsCount > 1) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Parcela ${transaction.currentInstallment}/${transaction.installmentsCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (transaction.isEssential) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Essencial",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (!transaction.notes.isNullOrBlank()) {
                        Text(
                            text = transaction.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (transaction.type == TransactionType.INCOME) "+ " else "- ") +
                            Formatters.formatCentsToCurrency(transaction.amountInCents),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (transaction.type == TransactionType.INCOME) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (amountInCents: Long, categoryId: Long, type: TransactionType, paymentMethod: PaymentMethod, isEssential: Boolean, installments: Int, notes: String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.PIX) }
    var isEssential by remember { mutableStateOf(false) }
    var installmentsText by remember { mutableStateOf("1") }
    var notesText by remember { mutableStateOf("") }

    val filteredCategories = categories.filter { it.type.name == selectedType.name }
    var selectedCategory by remember(selectedType, categories) {
        mutableStateOf(filteredCategories.firstOrNull())
    }
    var expandedCategoryMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Transação") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = {
                            selectedType = TransactionType.EXPENSE
                            selectedCategory = categories.firstOrNull { it.type == CategoryType.EXPENSE }
                        }
                    )
                    Text("Despesa")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = {
                            selectedType = TransactionType.INCOME
                            selectedCategory = categories.firstOrNull { it.type == CategoryType.INCOME }
                            isEssential = false
                        }
                    )
                    Text("Receita")
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor (R$) e.g. 150.00") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCategoryMenu,
                    onExpandedChange = { expandedCategoryMenu = !expandedCategoryMenu }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Selecione a categoria",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryMenu) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategoryMenu,
                        onDismissRequest = { expandedCategoryMenu = false }
                    ) {
                        filteredCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat
                                    expandedCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                // Payment Method
                Text("Forma de Pagamento:", style = MaterialTheme.typography.bodyMedium)
                val paymentMethodLabels = mapOf(
                    PaymentMethod.CASH to "Dinheiro",
                    PaymentMethod.DEBIT to "Débito",
                    PaymentMethod.CREDIT_CARD to "Cartão de Crédito",
                    PaymentMethod.PIX to "Pix"
                )
                Column {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf(PaymentMethod.CASH, PaymentMethod.DEBIT).forEach { method ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = selectedMethod == method,
                                    onClick = { selectedMethod = method }
                                )
                                Text(
                                    text = paymentMethodLabels[method] ?: method.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf(PaymentMethod.CREDIT_CARD, PaymentMethod.PIX).forEach { method ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = selectedMethod == method,
                                    onClick = { selectedMethod = method }
                                )
                                Text(
                                    text = paymentMethodLabels[method] ?: method.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                if (selectedMethod == PaymentMethod.CREDIT_CARD) {
                    OutlinedTextField(
                        value = installmentsText,
                        onValueChange = { installmentsText = it },
                        label = { Text("Número de Parcelas") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedType == TransactionType.EXPENSE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isEssential,
                            onCheckedChange = { isEssential = it }
                        )
                        Text("Despesa Essencial")
                    }
                }

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
                    val catId = selectedCategory?.id ?: 0L
                    val installments = installmentsText.toIntOrNull() ?: 1
                    if (amountInCents > 0 && catId > 0) {
                        onConfirm(amountInCents, catId, selectedType, selectedMethod, isEssential, installments, notesText.ifBlank { null })
                    }
                }
            ) {
                Text("Salvar")
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
fun DeleteTransactionDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onConfirm: (deleteGroup: Boolean) -> Unit
) {
    var deleteGroup by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir Transação") },
        text = {
            Column {
                Text("Deseja realmente excluir esta transação?")
                if (transaction.installmentGroupId != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = deleteGroup,
                            onCheckedChange = { deleteGroup = it }
                        )
                        Text("Excluir todas as parcelas deste parcelamento")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(deleteGroup) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Excluir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
