package com.example.kofre

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kofre.data.local.AppDatabase
import com.example.kofre.data.repository.FinanceRepositoryImpl
import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.domain.model.Category
import com.example.kofre.domain.model.Investment
import com.example.kofre.domain.model.Transaction
import com.example.kofre.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FinanceRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepositoryImpl(
            db.categoryDao(),
            db.transactionDao(),
            db.investmentDao(),
            db.investmentContributionDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testCategoryTreeMappingInRepository() = runBlocking {
        val parentId = repository.insertCategory(
            Category(name = "Moradia", type = CategoryType.EXPENSE)
        )
        repository.insertCategory(
            Category(name = "Aluguel", type = CategoryType.EXPENSE, parentId = parentId)
        )

        val categories = repository.getAllCategories().first()
        assertEquals(1, categories.size)
        assertEquals("Moradia", categories[0].name)
        assertEquals(1, categories[0].subcategories.size)
        assertEquals("Aluguel", categories[0].subcategories[0].name)
    }

    @Test
    fun testTransactionRepositoryInsertAndQuery() = runBlocking {
        val catId = repository.insertCategory(
            Category(name = "Saúde", type = CategoryType.EXPENSE)
        )
        val transaction = Transaction(
            amountInCents = 15000L,
            timestamp = 1000L,
            categoryId = catId,
            type = TransactionType.EXPENSE,
            paymentMethod = PaymentMethod.PIX
        )

        val txId = repository.insertTransaction(transaction)
        val list = repository.getAllTransactions().first()

        assertEquals(1, list.size)
        assertEquals(txId, list[0].id)
        assertNotNull(list[0].category)
        assertEquals("Saúde", list[0].category?.name)
    }

    @Test
    fun testInvestmentRepositoryOperations() = runBlocking {
        val inv = Investment(
            name = "Fundo Imobiliário",
            type = InvestmentType.VARIABLE,
            horizon = InvestmentHorizon.LONG,
            currentBalanceInCents = 50000L
        )

        val id = repository.insertInvestment(inv)
        repository.updateInvestmentBalance(id, 60000L)

        val investments = repository.getAllInvestments().first()
        assertEquals(1, investments.size)
        assertEquals(60000L, investments[0].currentBalanceInCents)
        assertEquals(InvestmentHorizon.LONG, investments[0].horizon)
    }
}
