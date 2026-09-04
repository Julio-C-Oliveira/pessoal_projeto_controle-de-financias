package com.example.kofre

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.room.Room
import com.example.kofre.data.local.AppDatabase
import com.example.kofre.data.repository.FinanceRepositoryImpl
import com.example.kofre.ui.navigation.AppNavigation
import com.example.kofre.ui.theme.KofreTheme

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
            database = database
        )

        setContent {
            KofreTheme {
                AppNavigation(repository = repository)
            }
        }
    }
}