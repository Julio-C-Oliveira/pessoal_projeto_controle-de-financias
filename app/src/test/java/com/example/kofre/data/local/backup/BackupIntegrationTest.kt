package com.example.kofre.data.local.backup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.kofre.data.local.AppDatabase
import com.example.kofre.data.local.entity.CategoryEntity
import com.example.kofre.data.local.entity.InvestmentContributionEntity
import com.example.kofre.data.local.entity.InvestmentEntity
import com.example.kofre.data.local.entity.MonthlyBudgetEntity
import com.example.kofre.data.local.entity.RecurringTransactionEntity
import com.example.kofre.data.local.entity.TransactionEntity
import com.example.kofre.data.local.enums.CategoryType
import com.example.kofre.data.local.enums.InvestmentHorizon
import com.example.kofre.data.local.enums.InvestmentType
import com.example.kofre.data.local.enums.PaymentMethod
import com.example.kofre.data.local.enums.RecurrenceFrequency
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
            recurringTransactionDao = db.recurringTransactionDao(),
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

        val recurringId = db.recurringTransactionDao().insert(
            RecurringTransactionEntity(
                id = 500L,
                amountInCents = 4500L,
                categoryId = childCatId,
                type = TransactionType.EXPENSE.name,
                paymentMethod = PaymentMethod.CREDIT_CARD.name,
                frequency = RecurrenceFrequency.MONTHLY.name,
                startDate = 1772614800000L,
                notes = "Assinatura mensal"
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
                recurringTransactionId = recurringId,
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

    // Teste 1 (Integração v2): Ciclo completo v2 — exportar banco contendo dados em todas as 6 entidades (inclusive recurringTransactions e transações com recurringTransactionId), limpar o banco, importar o arquivo v2 gerado e verificar que 100% dos registros foram restaurados exatamente com os mesmos IDs e atributos.
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
        db.recurringTransactionDao().deleteAllRecurringTransactions()
        db.investmentDao().deleteAllInvestments()
        db.categoryDao().deleteSubcategories()
        db.categoryDao().deleteAllCategories()

        assertEquals(0, db.categoryDao().getAllCategories().first().size)
        assertEquals(0, db.transactionDao().getAllTransactions().first().size)
        assertEquals(0, db.recurringTransactionDao().getAllRecurringTransactions().first().size)
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

        val recurringList = db.recurringTransactionDao().getAllRecurringTransactions().first()
        assertEquals(1, recurringList.size)
        assertEquals(500L, recurringList[0].id)
        assertEquals("Assinatura mensal", recurringList[0].notes)

        val transactions = db.transactionDao().getAllTransactions().first()
        assertEquals(1, transactions.size)
        assertEquals(1000L, transactions[0].id)
        assertEquals(4500L, transactions[0].amountInCents)
        assertEquals(2L, transactions[0].categoryId)
        assertEquals(500L, transactions[0].recurringTransactionId)

        val budgets = db.monthlyBudgetDao().getAllBudgets().first()
        assertEquals(1, budgets.size)
        assertEquals(50L, budgets[0].id)
        assertEquals(100000L, budgets[0].plannedAmountInCents)
    }

    // Teste 2 (Retrocompatibilidade v1): Importar arquivo de backup com version = 1 (sem a propriedade recurringTransactions) deve restaurar perfeitamente as categorias, transações, investimentos, aportes e orçamentos.
    @Test
    fun testImportBackupVersion1Retrocompatibility() = runBlocking {
        val v1PayloadJson = """
            {
              "version": 1,
              "exportedAt": 1772614800000,
              "categories": [
                { "id": 1, "name": "Alimentação", "type": "EXPENSE", "parentId": null }
              ],
              "transactions": [
                { "id": 100, "amountInCents": 2000, "timestamp": 1772614800000, "categoryId": 1, "type": "EXPENSE", "paymentMethod": "CASH", "isEssential": true, "installmentsCount": 1, "currentInstallment": 1 }
              ],
              "investments": [
                { "id": 10, "name": "CDB", "type": "FIXED_INCOME", "horizon": "SHORT", "currentBalanceInCents": 10000 }
              ],
              "investmentContributions": [
                { "id": 50, "investmentId": 10, "amountInCents": 10000, "timestamp": 1772614800000 }
              ],
              "monthlyBudgets": [
                { "id": 5, "year": 2026, "month": 9, "categoryId": 1, "plannedAmountInCents": 50000 }
              ]
            }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(v1PayloadJson.toByteArray(Charsets.UTF_8))
        val result = importBackupUseCase(inputStream)

        assertTrue("Esperava sucesso na importação de backup v1 mas falhou com: ${result.exceptionOrNull()}", result.isSuccess)

        val categories = db.categoryDao().getAllCategories().first()
        assertEquals(1, categories.size)
        assertEquals("Alimentação", categories[0].name)

        val transactions = db.transactionDao().getAllTransactions().first()
        assertEquals(1, transactions.size)
        assertEquals(100L, transactions[0].id)
        assertEquals(null, transactions[0].recurringTransactionId)

        val investments = db.investmentDao().getAllInvestments().first()
        assertEquals(1, investments.size)

        val contributions = db.investmentContributionDao().getAllContributions().first()
        assertEquals(1, contributions.size)

        val budgets = db.monthlyBudgetDao().getAllBudgets().first()
        assertEquals(1, budgets.size)
    }

    // Teste 3 (Versão Não Suportada): ImportBackupUseCase deve retornar Result.failure(UnsupportedBackupVersionException) ao receber payload com version > 2.
    @Test
    fun testImportBackupWithUnsupportedVersionFails() = runBlocking {
        val invalidPayloadJson = """
            {
              "version": 3,
              "exportedAt": 1772614800000,
              "categories": [],
              "transactions": [],
              "investments": [],
              "investmentContributions": [],
              "monthlyBudgets": [],
              "recurringTransactions": []
            }
        """.trimIndent()

        val inputStream = ByteArrayInputStream(invalidPayloadJson.toByteArray(Charsets.UTF_8))
        val result = importBackupUseCase(inputStream)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Exceção deveria ser UnsupportedBackupVersionException mas foi $exception", exception is UnsupportedBackupVersionException)
    }

    // Teste 4: Falha durante a inserção de transactions deve fazer rollback completo, deixando o banco intacto.
    @Test
    fun testFailedImportRollsBackCompletelyLeavingDatabaseIntact() = runBlocking {
        seedDatabase()

        val initialCategoriesCount = db.categoryDao().getAllCategories().first().size
        val initialTransactionsCount = db.transactionDao().getAllTransactions().first().size

        // Payload com transação apontando para categoryId 99999L que não existe nas categorias do payload nem no banco pós-wipe
        val invalidPayloadJson = """
            {
              "version": 2,
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
              "monthlyBudgets": [],
              "recurringTransactions": []
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

    // Teste 5: A restauração não deve lançar SQLiteConstraintException para um payload válido.
    @Test
    fun testValidPayloadImportDoesNotThrowConstraintException() = runBlocking {
        val validPayloadJson = """
            {
              "version": 2,
              "exportedAt": 1772614800000,
              "categories": [
                { "id": 1, "name": "Pai", "type": "EXPENSE", "parentId": null },
                { "id": 2, "name": "Filha", "type": "EXPENSE", "parentId": 1 }
              ],
              "investments": [
                { "id": 5, "name": "CDB", "type": "FIXED_INCOME", "horizon": "MEDIUM", "currentBalanceInCents": 20000 }
              ],
              "recurringTransactions": [
                { "id": 30, "amountInCents": 500, "categoryId": 2, "type": "EXPENSE", "paymentMethod": "CASH", "frequency": "MONTHLY", "startDate": 1772614800000, "generatedCount": 0, "isActive": true, "isEssential": true }
              ],
              "transactions": [
                { "id": 10, "amountInCents": 500, "timestamp": 1772614800000, "categoryId": 2, "type": "EXPENSE", "paymentMethod": "CASH", "isEssential": true, "installmentsCount": 1, "currentInstallment": 1, "recurringTransactionId": 30 }
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

    // Teste 6: O JSON exportado deve conter os campos: version = 2, exportedAt, categories, transactions, investments, investmentContributions, monthlyBudgets e recurringTransactions.
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
        assertTrue(jsonElement.containsKey("recurringTransactions"))

        assertEquals(2, jsonElement["version"]?.jsonPrimitive?.int)
    }
}
