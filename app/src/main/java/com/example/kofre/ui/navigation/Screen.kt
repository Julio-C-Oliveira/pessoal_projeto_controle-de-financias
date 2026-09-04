package com.example.kofre.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Início", Icons.Default.Home)
    object Transactions : Screen("transactions", "Extrato", Icons.Default.Receipt)
    object Budget : Screen("budget", "Orçamento", Icons.Default.PieChart)
    object Investments : Screen("investments", "Investimentos", Icons.Default.AccountBalanceWallet)
    object Reports : Screen("reports", "Relatórios", Icons.Default.Assessment)

    companion object {
        val items = listOf(Dashboard, Transactions, Budget, Investments, Reports)
    }
}
