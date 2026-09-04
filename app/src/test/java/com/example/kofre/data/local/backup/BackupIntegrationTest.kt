package com.example.kofre.data.local.backup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kofre.data.local.AppDatabase
import com.example.kofre.data.local.entity.CategoryEntity
import com.example.kofre.data.local.entity.InvestmentContributionEntity
import com.example.kofre.data.local.entity.InvestmentEntity
import com.example.kofre.data.local.entity.MonthlyBudgetEntity
import com.example.kofre.data.local.entity.TransactionEntity
import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.TransactionType
import com.example.kofre.data.repository.FinanceRepositoryImpl
import com.example.kofre.domain.usecase.backup.ExportBackupUseCaseImpl
import com.example.kofre.domain.usecase.backup.ImportBackupUseCaseImpl
import com.example.kofre.domain.usecase.backup.UnsupportedBackupVersionException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class BackupIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepositoryImpl
    private lateinit var exportBackupUseCase: ExportBackupUseCaseImpl
    private lateinit var importBackupUseCase: ImportBackupUseCaseImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        repository = FinanceRepositoryImpl(
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao(),
            investmentDao = db.investmentDao(),
            investmentContributionDao = db.investmentContributionDao(),
            monthlyBudgetDao = db.monthlyBudgetDao(),
            database = db
        )

        exportBackupUseCase = ExportBackupUseCaseImpl(repository)
        importBackupUseCase = ImportBackupUseCaseImpl(repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedDatabase() {
        val parentCatId = db.categoryDao().insertCategory(
            CategoryEntity(id = 1L, name = "Alimentação", type = CategoryType.EXPENSE.name, parentId = null)
        )
        val childCatId = db.categoryDao().insertCategory(
            CategoryEntity(id = 2L, name = "Restaurante", type = CategoryType.EXPENSE.name, parentId = parentCatId)
        )

        val investmentId = db.investmentDao().insertInvestment(
            InvestmentEntity(
                id = 10L,
                name = "Tesouro Selic",
                type = InvestmentType.FIXED_INCOME.name,
                horizon = InvestmentHorizon.SHORT.name,
                currentBalanceInCents = 500000L
            )
        )

        db.investmentContributionDao().insertContribution(
            InvestmentContributionEntity(
                id = 100L,
                investmentId = investmentId,
                amountInCents = 500000L,
                timestamp = 1772614800000L,
                notes = "Aporte inicial"
            )
        )

        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = 1000L,
                amountInCents = 4500L,
                timestamp = 1772614800000L,
                categoryId = childCatId,
                type = TransactionType.EXPENSE.name,
                paymentMethod = PaymentMethod.CREDIT_CARD.name,
                isEssential = false,
                notes = "Almoço"
            )
        )

        db.monthlyBudgetDao().upsertBudget(
            MonthlyBudgetEntity(
                id = 50L,
                year = 2026,
                month = 9,
                categoryId = parentCatId,
                plannedAmountInCents = 100000L
            )
        )
    }

    // Teste 1 (Integração): Ciclo completo — exportar banco com dados, limpar, importar e verificar que todos os registros foram restaurados com os mesmos IDs e valores.
    @Test
    fun testFullBackupAndRestoreCyclePreservesDataAndIds() = runBlocking {
        seedDatabase()

        val outputStream = ByteArrayOutputStream()
        val exportResult = exportBackupUseCase(outputStream)
        assertTrue(exportResult.isSuccess)

        val jsonBytes = outputStream.toByteArray()
        assertTrue(jsonBytes.isNotEmpty())

        // Limpar o banco de dados completamente
        db.monthlyBudgetDao().deleteAllBudgets()
        db.investmentContributionDao().deleteAllContributions()
        db.transactionDao().deleteAllTransactions()
        db.investmentDao().deleteAllInvestments()
        db.categoryDao().deleteSubcategories()
        db.categoryDao().deleteAllCategories()

        assertEquals(0, db.categoryDao().getAllCategories().first().size)
        assertEquals(0, db.transactionDao().getAllTransactions().first().size)
        assertEquals(0, db.investmentDao().getAllInvestments().first().size)
        assertEquals(0, db.investmentContributionDao().getAllContributions().first().size)
        assertEquals(0, db.monthlyBudgetDao().getAllBudgets().first().size)

        // Restaurar via import use case
        val inputStream = ByteArrayInputStream(jsonBytes)
        val importResult = importBackupUseCase(inputStream)
        assertTrue("Importação falhou: ${importResult.exceptionOrNull()}", importResult.isSuccess)

        // Verificar dados restaurados com exatos IDs e valores
        val categories = db.categoryDao().getAllCategories().first()
        assertEquals(2, categories.size)
        val parentCat = categories.find { it.id == 1L }
        assertNotNull(parentCat)
        assertEquals("Alimentação", parentCat!!.name)
        val childCat = categories.find { it.id == 2L }
        assertNotNull(childCat)
        assertEquals("Restaurante", childCat!!.name)
        assertEquals(1L, childCat.parentId)

        val investments = db.investmentDao().getAllInvestments().first()
        assertEquals(1, investments.size)
        assertEquals(10L, investments[0].id)
        assertEquals("Tesouro Selic", investments[0].name)
        assertEquals(500000L, investments[0].currentBalanceInCents)

        val contributions = db.investmentContributionDao().getAllContributions().first()
        assertEquals(1, contributions.size)
        assertEquals(100L, contributions[0].id)
        assertEquals(10L, contributions[0].investmentId)

        val transactions = db.transactionDao().getAllTransactions().first()
        assertEquals(1, transactions.size)
        assertEquals(1000L, transactions[0].id)
        assertEquals(4500L, transactions[0].amountInCents)
        assertEquals(2L, transactions[0].categoryId)

        val budgets = db.monthlyBudgetDao().getAllBudgets().first()
        assertEquals(1, budgets.size)
        assertEquals(50L, budgets[0].id)
        assertEquals(100000L, budgets[0].plannedAmountInCents)
    }

    // Teste 2: ImportBackupUseCase deve retornar Result.failure(UnsupportedBackupVersionException) ao receber payload com version > 1.
    @Test
    fun testImportBackupWithUnsupportedVersionFails() = runBlocking {
        val invalidPayloadJson = """
            {
              "version": 99,
              "exportedAt": 1772614800000,
              "categories": [],
              "transactions": [],
              "investments": [],
              "investmentContributions": [],
              "monthlyBudgets": []
            }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(invalidPayloadJson.toByteArray(Charsets.UTF_8))
        val result = importBackupUseCase(inputStream)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Exceção deveria ser UnsupportedBackupVersionException mas foi $exception", exception is UnsupportedBackupVersionException)
    }

    // Teste 3: Falha durante a inserção de transactions deve fazer rollback completo, deixando o banco intacto.
    @Test
    fun testFailedImportRollsBackCompletelyLeavingDatabaseIntact() = runBlocking {
        seedDatabase()

        val initialCategoriesCount = db.categoryDao().getAllCategories().first().size
        val initialTransactionsCount = db.transactionDao().getAllTransactions().first().size

        // Payload com transação apontando para categoryId 99999L que não existe nas categorias do payload nem no banco pós-wipe
        val invalidPayloadJson = """
            {
              "version": 1,
              "exportedAt": 1772614800000,
              "categories": [
                { "id": 100, "name": "Nova Categoria", "type": "EXPENSE", "parentId": null }
              ],
              "transactions": [
                {
                  "id": 500,
                  "amountInCents": 1000,
                  "timestamp": 1772614800000,
                  "categoryId": 99999,
                  "type": "EXPENSE",
                  "paymentMethod": "DEBIT",
                  "isEssential": false,
                  "installmentsCount": 1,
                  "currentInstallment": 1
                }
              ],
              "investments": [],
              "investmentContributions": [],
              "monthlyBudgets": []
            }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(invalidPayloadJson.toByteArray(Charsets.UTF_8))
        val result = importBackupUseCase(inputStream)

        assertTrue(result.isFailure)

        // Banco de dados deve ter sofrido rollback e mantido os dados anteriores intactos
        val postCategories = db.categoryDao().getAllCategories().first()
        val postTransactions = db.transactionDao().getAllTransactions().first()

        assertEquals(initialCategoriesCount, postCategories.size)
        assertEquals(initialTransactionsCount, postTransactions.size)
        assertEquals("Alimentação", postCategories.find { it.id == 1L }?.name)
    }

    // Teste 4: A restauração não deve lançar SQLiteConstraintException para um payload válido.
    @Test
    fun testValidPayloadImportDoesNotThrowConstraintException() = runBlocking {
        val validPayloadJson = """
            {
              "version": 1,
              "exportedAt": 1772614800000,
              "categories": [
                { "id": 1, "name": "Pai", "type": "EXPENSE", "parentId": null },
                { "id": 2, "name": "Filha", "type": "EXPENSE", "parentId": 1 }
              ],
              "investments": [
                { "id": 5, "name": "CDB", "type": "FIXED_INCOME", "horizon": "MEDIUM", "currentBalanceInCents": 20000 }
              ],
              "transactions": [
                { "id": 10, "amountInCents": 500, "timestamp": 1772614800000, "categoryId": 2, "type": "EXPENSE", "paymentMethod": "CASH", "isEssential": true, "installmentsCount": 1, "currentInstallment": 1 }
              ],
              "investmentContributions": [
                { "id": 15, "investmentId": 5, "amountInCents": 20000, "timestamp": 1772614800000 }
              ],
              "monthlyBudgets": [
                { "id": 20, "year": 2026, "month": 9, "categoryId": 1, "plannedAmountInCents": 50000 }
              ]
            }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(validPayloadJson.toByteArray(Charsets.UTF_8))
        val result = importBackupUseCase(inputStream)

        assertTrue("Esperava sucesso sem SQLiteConstraintException mas falhou com: ${result.exceptionOrNull()}", result.isSuccess)
    }

    // Teste 5: O JSON exportado deve conter os campos: version, exportedAt, categories, transactions, investments, investmentContributions e monthlyBudgets.
    @Test
    fun testExportedJsonContainsAllRequiredKeys() = runBlocking {
        seedDatabase()

        val outputStream = ByteArrayOutputStream()
        val result = exportBackupUseCase(outputStream)
        assertTrue("Exportação falhou com erro: ${result.exceptionOrNull()}", result.isSuccess)

        val jsonString = String(outputStream.toByteArray(), Charsets.UTF_8)
        val jsonElement = Json.parseToJsonElement(jsonString).jsonObject

        assertTrue(jsonElement.containsKey("version"))
        assertTrue(jsonElement.containsKey("exportedAt"))
        assertTrue(jsonElement.containsKey("categories"))
        assertTrue(jsonElement.containsKey("transactions"))
        assertTrue(jsonElement.containsKey("investments"))
        assertTrue(jsonElement.containsKey("investmentContributions"))
        assertTrue(jsonElement.containsKey("monthlyBudgets"))

        assertEquals(1, jsonElement["version"]?.jsonPrimitive?.int)
    }
}
