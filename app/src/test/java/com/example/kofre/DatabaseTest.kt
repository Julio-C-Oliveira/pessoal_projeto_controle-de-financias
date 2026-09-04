package com.example.kofre

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kofre.data.local.AppDatabase
import com.example.kofre.data.local.dao.CategoryDao
import com.example.kofre.data.local.dao.InvestmentDao
import com.example.kofre.data.local.dao.MonthlyBudgetDao
import com.example.kofre.data.local.dao.TransactionDao
import com.example.kofre.data.local.entity.CategoryEntity
import com.example.kofre.data.local.entity.InvestmentEntity
import com.example.kofre.data.local.entity.MonthlyBudgetEntity
import com.example.kofre.data.local.entity.TransactionEntity
import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class DatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var investmentDao: InvestmentDao
    private lateinit var monthlyBudgetDao: MonthlyBudgetDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        categoryDao = db.categoryDao()
        transactionDao = db.transactionDao()
        investmentDao = db.investmentDao()
        monthlyBudgetDao = db.monthlyBudgetDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // Teste 1: Inserir categoria e validar recuperação via Flow
    @Test
    fun testInsertCategoryAndRetrieveViaFlow() = runBlocking {
        val category = CategoryEntity(
            name = "Alimentação",
            type = CategoryType.EXPENSE.name
        )
        val id = categoryDao.insertCategory(category)
        assertTrue(id > 0)

        val categories = categoryDao.getAllCategories().first()
        assertEquals(1, categories.size)
        assertEquals("Alimentação", categories[0].name)
        assertEquals(CategoryType.EXPENSE.name, categories[0].type)
    }

    // Teste 2: Falhar com SQLiteConstraintException ao inserir transação com categoryId inexistente
    @Test
    fun testInsertTransactionWithNonExistentCategoryFails() = runBlocking {
        val transaction = TransactionEntity(
            amountInCents = 5000L,
            timestamp = System.currentTimeMillis(),
            categoryId = 999L, // Inexistente
            type = TransactionType.EXPENSE.name,
            paymentMethod = PaymentMethod.DEBIT.name
        )

        try {
            transactionDao.insertTransaction(transaction)
            fail("Deveria ter lançado SQLiteConstraintException devido à Foreign Key inexistente")
        } catch (e: SQLiteConstraintException) {
            // Sucesso
            assertNotNull(e)
        }
    }

    // Teste 3: Impedir a deleção de uma categoria que possua transações vinculadas (RESTRICT)
    @Test
    fun testDeleteCategoryWithLinkedTransactionsFails() = runBlocking {
        val category = CategoryEntity(
            name = "Transporte",
            type = CategoryType.EXPENSE.name
        )
        val categoryId = categoryDao.insertCategory(category)

        val transaction = TransactionEntity(
            amountInCents = 2000L,
            timestamp = System.currentTimeMillis(),
            categoryId = categoryId,
            type = TransactionType.EXPENSE.name,
            paymentMethod = PaymentMethod.CREDIT_CARD.name
        )
        transactionDao.insertTransaction(transaction)

        try {
            categoryDao.deleteCategory(category.copy(id = categoryId))
            fail("Deveria ter lançado SQLiteConstraintException ao tentar deletar categoria com transações vinculadas")
        } catch (e: SQLiteConstraintException) {
            // Sucesso (RESTRICT)
            assertNotNull(e)
        }
    }

    // Teste 4: Atualizar o saldo de um investimento e verificar a emissão do novo valor no Flow
    @Test
    fun testUpdateInvestmentBalanceEmitsNewValueInFlow() = runBlocking {
        val investment = InvestmentEntity(
            name = "Tesouro Selic",
            type = InvestmentType.FIXED_INCOME.name,
            horizon = InvestmentHorizon.SHORT.name,
            currentBalanceInCents = 100000L
        )
        val id = investmentDao.insertInvestment(investment)

        val initialList = investmentDao.getAllInvestments().first()
        assertEquals(100000L, initialList[0].currentBalanceInCents)

        investmentDao.updateBalance(id, 150000L)

        val updatedList = investmentDao.getAllInvestments().first()
        assertEquals(150000L, updatedList[0].currentBalanceInCents)
    }

    // Teste 5: Inserir 3 parcelas com o mesmo installmentGroupId e validar getTransactionsByGroupId
    @Test
    fun testInsertInstallmentsAndRetrieveByGroupId() = runBlocking {
        val categoryId = categoryDao.insertCategory(
            CategoryEntity(name = "Eletrônicos", type = CategoryType.EXPENSE.name)
        )
        val groupId = UUID.randomUUID().toString()

        val installments = (1..3).map { current ->
            TransactionEntity(
                amountInCents = 10000L,
                timestamp = System.currentTimeMillis() + (current * 86400000L),
                categoryId = categoryId,
                type = TransactionType.EXPENSE.name,
                paymentMethod = PaymentMethod.CREDIT_CARD.name,
                installmentGroupId = groupId,
                installmentsCount = 3,
                currentInstallment = current
            )
        }

        val insertedIds = transactionDao.insertTransactions(installments)
        assertEquals(3, insertedIds.size)

        val groupTransactions = transactionDao.getTransactionsByGroupId(groupId).first()
        assertEquals(3, groupTransactions.size)
        assertTrue(groupTransactions.all { it.installmentGroupId == groupId })
    }

    // Teste 6: Inserir orçamento e recuperar por mês
    @Test
    fun testInsertBudgetAndRetrieveByMonth() = runBlocking {
        val categoryId = categoryDao.insertCategory(
            CategoryEntity(name = "Alimentação", type = CategoryType.EXPENSE.name)
        )
        val budget = MonthlyBudgetEntity(
            year = 2026,
            month = 9,
            categoryId = categoryId,
            plannedAmountInCents = 60000L
        )
        val budgetId = monthlyBudgetDao.upsertBudget(budget)
        assertTrue(budgetId > 0)

        val budgets = monthlyBudgetDao.getBudgetsForMonth(2026, 9).first()
        assertEquals(1, budgets.size)
        assertEquals(60000L, budgets[0].plannedAmountInCents)
    }

    // Teste 7: Cascading delete de categoria remove orçamentos vinculados
    @Test
    fun testCascadeDeleteCategoryRemovesBudgets() = runBlocking {
        val categoryId = categoryDao.insertCategory(
            CategoryEntity(name = "Lazer", type = CategoryType.EXPENSE.name)
        )
        val budget = MonthlyBudgetEntity(
            year = 2026,
            month = 9,
            categoryId = categoryId,
            plannedAmountInCents = 30000L
        )
        monthlyBudgetDao.upsertBudget(budget)

        categoryDao.deleteCategory(CategoryEntity(id = categoryId, name = "Lazer", type = CategoryType.EXPENSE.name))

        val budgets = monthlyBudgetDao.getBudgetsForMonth(2026, 9).first()
        assertTrue(budgets.isEmpty())
    }
}

