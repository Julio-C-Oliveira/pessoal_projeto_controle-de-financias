package com.example.kofre

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.kofre.data.local.AppDatabase
import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.repository.FinanceRepositoryImpl
import com.example.kofre.domain.model.Category
import com.example.kofre.ui.navigation.AppNavigation
import com.example.kofre.ui.theme.KofreTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "kofre.db"
        ).fallbackToDestructiveMigration().build()

        val repository = FinanceRepositoryImpl(
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao(),
            investmentDao = database.investmentDao(),
            investmentContributionDao = database.investmentContributionDao(),
            monthlyBudgetDao = database.monthlyBudgetDao(),
            recurringTransactionDao = database.recurringTransactionDao(),
            database = database
        )

        lifecycleScope.launch {
            val existing = repository.getAllCategories().first()
            if (existing.isEmpty()) {
                val defaults = listOf(
                    Category(name = "Alimentação", type = CategoryType.EXPENSE),
                    Category(name = "Moradia", type = CategoryType.EXPENSE),
                    Category(name = "Transporte", type = CategoryType.EXPENSE),
                    Category(name = "Saúde", type = CategoryType.EXPENSE),
                    Category(name = "Educação", type = CategoryType.EXPENSE),
                    Category(name = "Lazer", type = CategoryType.EXPENSE),
                    Category(name = "Vestuário", type = CategoryType.EXPENSE),
                    Category(name = "Serviços", type = CategoryType.EXPENSE),
                    Category(name = "Outros", type = CategoryType.EXPENSE),
                    Category(name = "Salário", type = CategoryType.INCOME),
                    Category(name = "Freelance", type = CategoryType.INCOME),
                    Category(name = "Investimentos", type = CategoryType.INCOME),
                    Category(name = "Aluguel Recebido", type = CategoryType.INCOME),
                    Category(name = "Outros", type = CategoryType.INCOME)
                )
                defaults.forEach { repository.insertCategory(it) }
            }
        }

        setContent {
            KofreTheme {
                AppNavigation(repository = repository)
            }
        }
    }
}