package com.example.kofre.ui.navigation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kofre.domain.repository.FinanceRepository
import com.example.kofre.domain.usecase.backup.ExportBackupUseCaseImpl
import com.example.kofre.domain.usecase.backup.ImportBackupUseCaseImpl
import com.example.kofre.domain.usecase.budget.GetMonthlyBudgetOverviewUseCaseImpl
import com.example.kofre.domain.usecase.budget.SetCategoryBudgetUseCaseImpl
import com.example.kofre.domain.usecase.category.AddCategoryUseCaseImpl
import com.example.kofre.domain.usecase.category.DeleteCategoryUseCaseImpl
import com.example.kofre.domain.usecase.category.GetCategoriesUseCaseImpl
import com.example.kofre.domain.usecase.investment.AddContributionUseCaseImpl
import com.example.kofre.domain.usecase.investment.CreateInvestmentUseCaseImpl
import com.example.kofre.domain.usecase.investment.DeleteInvestmentUseCaseImpl
import com.example.kofre.domain.usecase.investment.GetInvestmentsSummaryUseCaseImpl
import com.example.kofre.domain.usecase.investment.UpdateInvestmentBalanceUseCaseImpl
import com.example.kofre.domain.usecase.report.GetFinancialReportUseCaseImpl
import com.example.kofre.domain.usecase.transaction.CreateTransactionUseCaseImpl
import com.example.kofre.domain.usecase.transaction.DeleteTransactionUseCaseImpl
import com.example.kofre.ui.screens.backup.BackupScreen
import com.example.kofre.ui.screens.backup.BackupViewModel
import com.example.kofre.ui.screens.budget.BudgetScreen
import com.example.kofre.ui.screens.budget.BudgetViewModel
import com.example.kofre.ui.screens.categories.CategoriesScreen
import com.example.kofre.ui.screens.categories.CategoriesViewModel
import com.example.kofre.ui.screens.dashboard.DashboardScreen
import com.example.kofre.ui.screens.dashboard.DashboardViewModel
import com.example.kofre.ui.screens.investments.InvestmentsScreen
import com.example.kofre.ui.screens.investments.InvestmentsViewModel
import com.example.kofre.ui.screens.reports.ReportsScreen
import com.example.kofre.ui.screens.reports.ReportsViewModel
import com.example.kofre.ui.screens.transactions.TransactionsScreen
import com.example.kofre.ui.screens.transactions.TransactionsViewModel

@Composable
fun AppNavigation(
    repository: FinanceRepository,
    navController: NavHostController = rememberNavController()
) {
    // Instantiate use cases
    val getFinancialReportUseCase = GetFinancialReportUseCaseImpl(repository)
    val createTransactionUseCase = CreateTransactionUseCaseImpl(repository)
    val deleteTransactionUseCase = DeleteTransactionUseCaseImpl(repository)
    val getMonthlyBudgetOverviewUseCase = GetMonthlyBudgetOverviewUseCaseImpl(repository)
    val setCategoryBudgetUseCase = SetCategoryBudgetUseCaseImpl(repository)
    val getInvestmentsSummaryUseCase = GetInvestmentsSummaryUseCaseImpl(repository)
    val createInvestmentUseCase = CreateInvestmentUseCaseImpl(repository)
    val addContributionUseCase = AddContributionUseCaseImpl(repository)
    val updateInvestmentBalanceUseCase = UpdateInvestmentBalanceUseCaseImpl(repository)
    val deleteInvestmentUseCase = DeleteInvestmentUseCaseImpl(repository)
    val exportBackupUseCase = ExportBackupUseCaseImpl(repository)
    val importBackupUseCase = ImportBackupUseCaseImpl(repository)
    val getCategoriesUseCase = GetCategoriesUseCaseImpl(repository)
    val addCategoryUseCase = AddCategoryUseCaseImpl(repository)
    val deleteCategoryUseCase = DeleteCategoryUseCaseImpl(repository)

    // Instantiate ViewModels
    val dashboardViewModel = DashboardViewModel(repository, getFinancialReportUseCase)
    val transactionsViewModel = TransactionsViewModel(repository, createTransactionUseCase, deleteTransactionUseCase)
    val budgetViewModel = BudgetViewModel(repository, getMonthlyBudgetOverviewUseCase, setCategoryBudgetUseCase)
    val investmentsViewModel = InvestmentsViewModel(
        repository,
        getInvestmentsSummaryUseCase,
        createInvestmentUseCase,
        addContributionUseCase,
        updateInvestmentBalanceUseCase,
        deleteInvestmentUseCase
    )
    val categoriesViewModel = CategoriesViewModel(
        getCategoriesUseCase,
        addCategoryUseCase,
        deleteCategoryUseCase
    )
    val reportsViewModel = ReportsViewModel(getFinancialReportUseCase)
    val backupViewModel = BackupViewModel(exportBackupUseCase, importBackupUseCase)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = NavigationBarDefaults.containerColor,
                tonalElevation = NavigationBarDefaults.Elevation
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Screen.items.forEach { screen ->
                        NavigationBarItem(
                            modifier = Modifier.widthIn(min = 88.dp),
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = {
                                Text(
                                    text = screen.title,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(viewModel = dashboardViewModel)
            }
            composable(Screen.Transactions.route) {
                TransactionsScreen(viewModel = transactionsViewModel)
            }
            composable(Screen.Budget.route) {
                BudgetScreen(viewModel = budgetViewModel)
            }
            composable(Screen.Investments.route) {
                InvestmentsScreen(viewModel = investmentsViewModel)
            }
            composable(Screen.Categories.route) {
                CategoriesScreen(viewModel = categoriesViewModel)
            }
            composable(Screen.Reports.route) {
                ReportsScreen(viewModel = reportsViewModel)
            }
            composable(Screen.Backup.route) {
                BackupScreen(viewModel = backupViewModel)
            }
        }
    }
}
