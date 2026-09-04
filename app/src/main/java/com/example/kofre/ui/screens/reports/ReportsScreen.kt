package com.example.kofre.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kofre.domain.usecase.report.CategoryExpenseSummary
import com.example.kofre.domain.usecase.report.PeriodType
import com.example.kofre.ui.util.Formatters

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading || state.report == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val report = state.report!!

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Relatórios Analíticos",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Temporal Scope Selector
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                PeriodType.values().forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = state.filter.periodType == type,
                        onClick = { viewModel.selectPeriodType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = PeriodType.values().size)
                    ) {
                        Text(
                            when (type) {
                                PeriodType.WEEK -> "Semana"
                                PeriodType.MONTH -> "Mês"
                                PeriodType.YEAR -> "Ano"
                            }
                        )
                    }
                }
            }
        }

        // Navigation Bar (< Date >)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.navigatePeriod(forward = false) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Anterior")
                    }
                    Text(
                        text = Formatters.formatDate(state.filter.referenceDate),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { viewModel.navigatePeriod(forward = true) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Próximo")
                    }
                }
            }
        }

        // Consolidated Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Consolidado do Período",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Receitas Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.formatCentsToCurrency(report.totalIncomeInCents), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Despesas Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.formatCentsToCurrency(report.totalExpenseInCents), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFC62828))
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Saldo Líquido", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.formatCentsToCurrency(report.netBalanceInCents), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Aportes em Ativos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.formatCentsToCurrency(report.totalInvestedInCents), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Capacidade de Poupança / Caixa Livre",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatCentsToCurrency(report.freeCashInCents),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Essential vs Non-Essential Breakdown Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Qualidade dos Gastos (Essencial vs Não-Essencial)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val totalExp = if (report.totalExpenseInCents > 0) report.totalExpenseInCents.toFloat() else 1f
                    val essentialWeight = (report.essentialExpenseInCents.toFloat() / totalExp).coerceIn(0f, 1f)
                    val nonEssentialWeight = (report.nonEssentialExpenseInCents.toFloat() / totalExp).coerceIn(0f, 1f)

                    // Proportions Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        if (essentialWeight > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(essentialWeight)
                                    .fillMaxHeight()
                                    .background(Color(0xFF0288D1))
                            )
                        }
                        if (nonEssentialWeight > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(nonEssentialWeight)
                                    .fillMaxHeight()
                                    .background(Color(0xFFE65100))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .height(10.dp)
                                    .width(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF0288D1))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Essenciais: ${Formatters.formatCentsToCurrency(report.essentialExpenseInCents)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .height(10.dp)
                                    .width(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFE65100))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Não-Essenciais: ${Formatters.formatCentsToCurrency(report.nonEssentialExpenseInCents)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        // Category Expenses Breakdown
        item {
            Text(
                text = "Despesas por Categoria",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (report.categoryExpenses.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Nenhuma despesa registrada neste período.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(report.categoryExpenses) { catExpense ->
                CategoryExpenseRow(catExpense)
            }
        }
    }
}

@Composable
fun CategoryExpenseRow(item: CategoryExpenseSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${Formatters.formatCentsToCurrency(item.totalInCents)} (${String.format("%.1f", item.percentageOfTotal)}%)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Proportion Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (item.percentageOfTotal / 100.0).toFloat().coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
