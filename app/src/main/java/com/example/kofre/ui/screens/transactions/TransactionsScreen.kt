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
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.RecurrenceFrequency
import com.example.kofre.data.local.enums.TransactionType
import java.time.DayOfWeek
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.model.RecurringTransaction
import com.example.kofre.domain.model.Transaction
import com.example.kofre.ui.util.Formatters
import kotlinx.coroutines.launch

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var recurringToDelete by remember { mutableStateOf<RecurringTransaction?>(null) }
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
                text = "Gestão de Transações",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))

            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Extrato (${state.transactions.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Recorrentes (${state.recurringTransactions.size})") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTabIndex == 0) {
                // Transactions List
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
            } else {
                // Recurring Rules List
                if (state.recurringTransactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Nenhuma regra de recorrência cadastrada.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.recurringTransactions) { recurring ->
                            RecurringTransactionCard(
                                recurring = recurring,
                                onToggleActive = { isActive ->
                                    coroutineScope.launch {
                                        viewModel.toggleRecurringTransaction(recurring.id, isActive)
                                    }
                                },
                                onDeleteClick = { recurringToDelete = recurring }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            categories = state.categories,
            externalErrorMessage = state.errorMessage,
            onDismiss = { showAddDialog = false },
            onConfirmNormal = { amountInCents, catId, type, method, isEssential, installments, timestamp, notes ->
                coroutineScope.launch {
                    val success = viewModel.createTransaction(
                        amountInCents = amountInCents,
                        categoryId = catId,
                        type = type,
                        paymentMethod = method,
                        isEssential = isEssential,
                        installmentsCount = installments,
                        timestamp = timestamp ?: System.currentTimeMillis(),
                        notes = notes
                    )
                    if (success) {
                        showAddDialog = false
                        selectedTabIndex = 0
                    }
                }
            },
            onConfirmRecurring = { amountInCents, catId, type, method, frequency, isEssential, startDate, totalOccurrences, notes ->
                coroutineScope.launch {
                    val success = viewModel.createRecurringTransaction(
                        amountInCents = amountInCents,
                        categoryId = catId,
                        type = type,
                        paymentMethod = method,
                        frequency = frequency,
                        isEssential = isEssential,
                        startDate = startDate ?: System.currentTimeMillis(),
                        totalOccurrences = totalOccurrences,
                        notes = notes
                    )
                    if (success) {
                        showAddDialog = false
                        selectedTabIndex = 1
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

    recurringToDelete?.let { recurring ->
        DeleteRecurringRuleDialog(
            recurring = recurring,
            onDismiss = { recurringToDelete = null },
            onConfirm = {
                coroutineScope.launch {
                    viewModel.deleteRecurringTransactionRule(recurring.id)
                    recurringToDelete = null
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
                    if (transaction.recurringTransactionId != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Recorrente",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
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

@Composable
fun RecurringTransactionCard(
    recurring: RecurringTransaction,
    onToggleActive: (Boolean) -> Unit,
    onDeleteClick: () -> Unit
) {
    val frequencyLabel = when (recurring.frequency) {
        RecurrenceFrequency.MONTHLY -> "Mensal"
        RecurrenceFrequency.WEEKLY -> "Semanal"
        RecurrenceFrequency.YEARLY -> "Anual"
    }

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
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = recurring.category?.name ?: "Categoria ${recurring.categoryId}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "$frequencyLabel • ${recurring.paymentMethod.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (recurring.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (recurring.isActive) "Ativa" else "Pausada",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (recurring.isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (recurring.isEssential) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "Essencial",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    val periodText = if (recurring.totalOccurrences != null) {
                        "Desde ${Formatters.formatDate(recurring.startDate)} (${recurring.generatedCount}/${recurring.totalOccurrences} ocorrências)"
                    } else {
                        "Desde ${Formatters.formatDate(recurring.startDate)} (Tempo indeterminado)"
                    }
                    Text(
                        text = periodText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (!recurring.notes.isNullOrBlank()) {
                        Text(
                            text = recurring.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (recurring.type == TransactionType.INCOME) "+ " else "- ") +
                            Formatters.formatCentsToCurrency(recurring.amountInCents),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (recurring.type == TransactionType.INCOME) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = recurring.isActive,
                        onCheckedChange = onToggleActive
                    )
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir Regra",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    categories: List<Category>,
    externalErrorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirmNormal: (amountInCents: Long, categoryId: Long, type: TransactionType, paymentMethod: PaymentMethod, isEssential: Boolean, installments: Int, timestamp: Long?, notes: String?) -> Unit,
    onConfirmRecurring: (amountInCents: Long, categoryId: Long, type: TransactionType, paymentMethod: PaymentMethod, frequency: RecurrenceFrequency, isEssential: Boolean, startDate: Long?, totalOccurrences: Int?, notes: String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.PIX) }
    var isEssential by remember { mutableStateOf(false) }
    var installmentsText by remember { mutableStateOf("1") }
    var notesText by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Date selection
    var useSpecificDate by remember { mutableStateOf(false) }
    var dateText by remember { mutableStateOf("") }

    // Recurrence duration selection
    var isIndefiniteDuration by remember { mutableStateOf(true) }
    var occurrencesText by remember { mutableStateOf("2") }

    // Recurrence day/date selection
    val nowLocalDate = remember { java.time.LocalDate.now() }
    var selectedDayOfWeek by remember { mutableStateOf(nowLocalDate.dayOfWeek) }
    var dayOfMonthText by remember { mutableStateOf(nowLocalDate.dayOfMonth.toString()) }
    var dayOfYearText by remember {
        mutableStateOf(String.format(java.util.Locale.US, "%02d/%02d", nowLocalDate.dayOfMonth, nowLocalDate.monthValue))
    }

    val allFlatCategories = remember(categories) { flattenCategories(categories) }
    val filteredCategories = remember(selectedType, allFlatCategories) {
        allFlatCategories.filter { it.type.name == selectedType.name }
    }
    var selectedCategory by remember(selectedType, categories) {
        mutableStateOf(filteredCategories.firstOrNull())
    }
    var expandedCategoryMenu by remember { mutableStateOf(false) }

    val activeErrorMessage = validationError ?: externalErrorMessage
    val dialogScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isRecurring) "Nova Transação Recorrente" else "Nova Transação") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(dialogScrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeErrorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = activeErrorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Type selection
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = {
                            selectedType = TransactionType.EXPENSE
                            selectedCategory = allFlatCategories.firstOrNull { it.type == CategoryType.EXPENSE }
                            validationError = null
                        }
                    )
                    Text("Despesa")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = {
                            selectedType = TransactionType.INCOME
                            selectedCategory = allFlatCategories.firstOrNull { it.type == CategoryType.INCOME }
                            isEssential = false
                            validationError = null
                        }
                    )
                    Text("Receita")
                }

                // Amount field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor (R$) e.g. 150.00 ou 700") },
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

                // Date selection: Hoje vs Data Específica
                Text("Data da Transação:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = !useSpecificDate,
                        onClick = { useSpecificDate = false }
                    )
                    Text("Hoje")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = useSpecificDate,
                        onClick = { useSpecificDate = true }
                    )
                    Text("Data Específica")
                }
                if (useSpecificDate) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Data (DD/MM/AAAA ou MM/AAAA) ex: 04/09/2026") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Recurrence Checkbox
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                    Text("Tornar Recorrente", fontWeight = FontWeight.SemiBold)
                }

                if (isRecurring) {
                    // 1. Frequency Selection
                    Text("Frequência de Recorrência:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = selectedFrequency == RecurrenceFrequency.MONTHLY,
                                    onClick = { selectedFrequency = RecurrenceFrequency.MONTHLY }
                                )
                                Text("Mensal")
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = selectedFrequency == RecurrenceFrequency.WEEKLY,
                                    onClick = { selectedFrequency = RecurrenceFrequency.WEEKLY }
                                )
                                Text("Semanal")
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedFrequency == RecurrenceFrequency.YEARLY,
                                onClick = { selectedFrequency = RecurrenceFrequency.YEARLY }
                            )
                            Text("Anual")
                        }
                    }

                    // 2. Specific Recurrence Day Selection
                    when (selectedFrequency) {
                        RecurrenceFrequency.WEEKLY -> {
                            Text("Dia da semana em que ocorre:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            val daysOfWeek = listOf(
                                DayOfWeek.MONDAY to "Seg",
                                DayOfWeek.TUESDAY to "Ter",
                                DayOfWeek.WEDNESDAY to "Qua",
                                DayOfWeek.THURSDAY to "Qui",
                                DayOfWeek.FRIDAY to "Sex",
                                DayOfWeek.SATURDAY to "Sáb",
                                DayOfWeek.SUNDAY to "Dom"
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                    for ((day, label) in daysOfWeek.take(4)) {
                                        FilterChip(
                                            selected = selectedDayOfWeek == day,
                                            onClick = { selectedDayOfWeek = day },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                                    for ((day, label) in daysOfWeek.drop(4)) {
                                        FilterChip(
                                            selected = selectedDayOfWeek == day,
                                            onClick = { selectedDayOfWeek = day },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                        RecurrenceFrequency.MONTHLY -> {
                            OutlinedTextField(
                                value = dayOfMonthText,
                                onValueChange = { dayOfMonthText = it.filter { c -> c.isDigit() } },
                                label = { Text("Dia do mês em que ocorre (1 a 31)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        RecurrenceFrequency.YEARLY -> {
                            OutlinedTextField(
                                value = dayOfYearText,
                                onValueChange = { dayOfYearText = it },
                                label = { Text("Dia e Mês em que ocorre (DD/MM), ex: 25/12") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 3. Duration selection: Tempo Indeterminado vs Período Específico
                    Text("Duração da Recorrência:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isIndefiniteDuration,
                                onClick = { isIndefiniteDuration = true }
                            )
                            Text("Tempo Indeterminado")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = !isIndefiniteDuration,
                                onClick = { isIndefiniteDuration = false }
                            )
                            Text("Período Específico")
                        }
                    }

                    if (!isIndefiniteDuration) {
                        val unitLabel = when (selectedFrequency) {
                            RecurrenceFrequency.MONTHLY -> "meses"
                            RecurrenceFrequency.WEEKLY -> "semanas"
                            RecurrenceFrequency.YEARLY -> "anos"
                        }
                        Text(
                            text = "Repetir por quantos $unitLabel:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                for (count in listOf(2, 3, 4)) {
                                    FilterChip(
                                        selected = occurrencesText == count.toString(),
                                        onClick = { occurrencesText = count.toString() },
                                        label = { Text("${count}x", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                for (count in listOf(5, 12)) {
                                    FilterChip(
                                        selected = occurrencesText == count.toString(),
                                        onClick = { occurrencesText = count.toString() },
                                        label = { Text("${count}x", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        OutlinedTextField(
                            value = occurrencesText,
                            onValueChange = { occurrencesText = it.filter { char -> char.isDigit() } },
                            label = { Text("Número de $unitLabel (ex: 2, 6, 12, 24...)") },
                            modifier = Modifier.fillMaxWidth()
                        )
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

                if (!isRecurring && selectedMethod == PaymentMethod.CREDIT_CARD) {
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
                    val amountInCents = parseAmountToCents(amountText)
                    val currentCategory = selectedCategory ?: filteredCategories.firstOrNull()
                    val catId = currentCategory?.id ?: 0L

                    if (amountInCents <= 0) {
                        validationError = "Informe um valor maior que zero (ex: 150.00 ou 700)."
                        coroutineScope.launch { dialogScrollState.animateScrollTo(0) }
                        return@Button
                    }
                    if (catId <= 0) {
                        validationError = "Selecione uma categoria válida."
                        coroutineScope.launch { dialogScrollState.animateScrollTo(0) }
                        return@Button
                    }

                    val customDateParsed = if (useSpecificDate) parseDateToMillis(dateText) else null
                    if (useSpecificDate && customDateParsed == null) {
                        validationError = "Informe uma data válida (ex: 04/09/2026 ou 09/2026)."
                        coroutineScope.launch { dialogScrollState.animateScrollTo(0) }
                        return@Button
                    }

                    if (isRecurring) {
                        if (selectedFrequency == RecurrenceFrequency.MONTHLY) {
                            val day = dayOfMonthText.toIntOrNull()
                            if (day == null || day !in 1..31) {
                                validationError = "Informe um dia do mês válido (entre 1 e 31)."
                                coroutineScope.launch { dialogScrollState.animateScrollTo(0) }
                                return@Button
                            }
                        } else if (selectedFrequency == RecurrenceFrequency.YEARLY) {
                            val parts = dayOfYearText.split("/")
                            val day = parts.getOrNull(0)?.trim()?.toIntOrNull()
                            val month = parts.getOrNull(1)?.trim()?.toIntOrNull()
                            if (parts.size != 2 || day == null || month == null || day !in 1..31 || month !in 1..12) {
                                validationError = "Informe o dia e mês da recorrência no formato DD/MM (ex: 25/12)."
                                coroutineScope.launch { dialogScrollState.animateScrollTo(0) }
                                return@Button
                            }
                        }

                        if (!isIndefiniteDuration) {
                            val occ = occurrencesText.toIntOrNull()
                            if (occ == null || occ < 2) {
                                validationError = "Informe um número de repetições válido (no mínimo 2)."
                                coroutineScope.launch { dialogScrollState.animateScrollTo(0) }
                                return@Button
                            }
                        }
                    }

                    validationError = null
                    val installments = installmentsText.toIntOrNull() ?: 1
                    val occurrencesParsed = occurrencesText.toIntOrNull()?.takeIf { it > 1 } ?: 2

                    if (isRecurring) {
                        val totalOcc = if (isIndefiniteDuration) null else occurrencesParsed
                        val recurrenceStartDate = calculateRecurrenceStartDate(
                            selectedFrequency,
                            selectedDayOfWeek,
                            dayOfMonthText,
                            dayOfYearText,
                            customDateParsed
                        )
                        onConfirmRecurring(
                            amountInCents,
                            catId,
                            selectedType,
                            selectedMethod,
                            selectedFrequency,
                            isEssential,
                            recurrenceStartDate,
                            totalOcc,
                            notesText.ifBlank { null }
                        )
                    } else {
                        onConfirmNormal(
                            amountInCents,
                            catId,
                            selectedType,
                            selectedMethod,
                            isEssential,
                            installments,
                            customDateParsed,
                            notesText.ifBlank { null }
                        )
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

private fun parseAmountToCents(text: String): Long {
    val clean = text.replace("R$", "").replace(" ", "").trim()
    if (clean.isBlank()) return 0L

    val normalized = if (clean.contains(",") && clean.contains(".")) {
        clean.replace(".", "").replace(",", ".")
    } else if (clean.contains(",")) {
        clean.replace(",", ".")
    } else {
        clean
    }

    val valDouble = normalized.toDoubleOrNull() ?: return 0L
    return (valDouble * 100).toLong()
}

private fun calculateRecurrenceStartDate(
    frequency: RecurrenceFrequency,
    selectedDayOfWeek: DayOfWeek,
    dayOfMonthText: String,
    dayOfYearText: String,
    customBaseDateMillis: Long?
): Long {
    val zoneId = java.time.ZoneId.systemDefault()
    val baseLocalDate = if (customBaseDateMillis != null) {
        java.time.Instant.ofEpochMilli(customBaseDateMillis).atZone(zoneId).toLocalDate()
    } else {
        java.time.LocalDate.now()
    }

    return when (frequency) {
        RecurrenceFrequency.WEEKLY -> {
            var date = baseLocalDate
            while (date.dayOfWeek != selectedDayOfWeek) {
                date = date.plusDays(1)
            }
            date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        }
        RecurrenceFrequency.MONTHLY -> {
            val dayInt = dayOfMonthText.toIntOrNull()?.coerceIn(1, 31) ?: baseLocalDate.dayOfMonth
            var date = try {
                baseLocalDate.withDayOfMonth(dayInt.coerceAtMost(baseLocalDate.lengthOfMonth()))
            } catch (e: Exception) {
                baseLocalDate
            }
            if (customBaseDateMillis == null && date.isBefore(baseLocalDate)) {
                val nextMonth = baseLocalDate.plusMonths(1)
                date = nextMonth.withDayOfMonth(dayInt.coerceAtMost(nextMonth.lengthOfMonth()))
            }
            date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        }
        RecurrenceFrequency.YEARLY -> {
            val parts = dayOfYearText.split("/")
            if (parts.size == 2) {
                val day = parts[0].trim().toIntOrNull() ?: baseLocalDate.dayOfMonth
                val month = parts[1].trim().toIntOrNull() ?: baseLocalDate.monthValue
                var date = try {
                    java.time.LocalDate.of(baseLocalDate.year, month.coerceIn(1, 12), day.coerceIn(1, 31))
                } catch (e: Exception) {
                    baseLocalDate
                }
                if (customBaseDateMillis == null && date.isBefore(baseLocalDate)) {
                    date = date.plusYears(1)
                }
                date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            } else {
                baseLocalDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            }
        }
    }
}

private fun parseDateToMillis(text: String): Long? {
    if (text.isBlank()) return null
    return try {
        val parts = text.split("/")
        if (parts.size == 3) {
            val day = parts[0].trim().toInt()
            val month = parts[1].trim().toInt()
            val rawYear = parts[2].trim().toInt()
            val year = if (rawYear < 100) 2000 + rawYear else rawYear
            java.time.LocalDate.of(year, month, day).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else if (parts.size == 2) {
            val p0 = parts[0].trim().toInt()
            val p1 = parts[1].trim().toInt()
            if (p0 <= 12 && p1 > 12) {
                val year = if (p1 < 100) 2000 + p1 else p1
                java.time.LocalDate.of(year, p0, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            } else {
                null
            }
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
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

@Composable
fun DeleteRecurringRuleDialog(
    recurring: RecurringTransaction,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir Regra de Recorrência") },
        text = {
            Text("Deseja excluir esta regra de recorrência? Os lançamentos já gerados no extrato serão mantidos.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
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

private fun flattenCategories(categories: List<Category>): List<Category> {
    val result = mutableListOf<Category>()
    for (cat in categories) {
        result.add(cat)
        if (cat.subcategories.isNotEmpty()) {
            result.addAll(flattenCategories(cat.subcategories))
        }
    }
    return result
}
