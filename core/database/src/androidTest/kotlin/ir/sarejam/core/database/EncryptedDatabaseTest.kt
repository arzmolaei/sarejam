package ir.sarejam.core.database

import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Uses real app-private files and the native cipher; never substitutes an in-memory/mock database. */
@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "encrypted-test-${UUID.randomUUID()}.db"
    private val key = ByteArray(32).also(java.security.SecureRandom()::nextBytes)
    private var database: SarejamDatabase? = null

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SarejamDatabase::class.java,
        emptyList(),
        SupportOpenHelperFactory(key.copyOf()),
    )

    @After
    fun cleanUp() {
        database?.close()
        context.deleteDatabase(name)
        key.fill(0)
    }

    private fun open(): SarejamDatabase = SarejamDatabase.open(context, key, name).also { database = it }

    @Test
    fun accountPersistsAcrossCloseAndReopenWithoutMutatingCallerKey() = runBlocking {
        val originalKey = key.copyOf()
        val account = account()
        open().accountDao().insert(account)
        database!!.close()
        assertTrue(originalKey.contentEquals(key))
        assertEquals(account, open().accountDao().findById(account.id))
        originalKey.fill(0)
    }

    @Test
    fun wrongKeyCannotOpenExistingFileAndDoesNotReplaceItsData() = runBlocking {
        val account = account()
        open().accountDao().insert(account)
        database!!.close()
        val wrongKey = ByteArray(32) { 42 }
        val wrongDatabase = SarejamDatabase.open(context, wrongKey, name)
        try {
            expectFailure { wrongDatabase.openHelper.writableDatabase }
        } finally {
            wrongDatabase.close()
            wrongKey.fill(0)
        }
        assertEquals(account, open().accountDao().findById(account.id))
    }

    @Test
    fun stockAndroidSqliteCannotReadEncryptedFile() = runBlocking {
        open().accountDao().insert(account())
        database!!.close()
        expectFailure {
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READONLY)
                .use { sqlite -> sqlite.rawQuery("SELECT count(*) FROM accounts", null).use { it.moveToFirst() } }
        }
        val header = ByteArray(16)
        context.getDatabasePath(name).inputStream().use { assertEquals(16, it.read(header)) }
        assertFalse(header.contentEquals("SQLite format 3\u0000".toByteArray()))
    }

    @Test
    fun accountDefaultsAndArchivePreserveTransactionHistory() = runBlocking {
        val db = open()
        val account = account()
        db.accountDao().insert(account)
        val transaction = transaction(account.id)
        db.transactionDao().insert(transaction)
        db.accountDao().setArchived(account.id, true, 200L)
        val archived = db.accountDao().findById(account.id)!!
        assertEquals("IRR", archived.currency)
        assertTrue(archived.includedInSpendable)
        assertTrue(archived.archived)
        assertEquals(200L, archived.updatedAt)
        assertEquals(listOf(transaction), db.transactionDao().listForAccount(account.id))
        assertEquals(emptyList<AccountEntity>(), db.accountDao().observeActive().first())
        expectFailure { db.openHelper.writableDatabase.execSQL("DELETE FROM accounts WHERE id = ?", arrayOf(account.id)) }
        assertEquals(account.id, db.accountDao().listAll().single().id)
    }

    @Test
    fun schemaDefaultsApplyToSqlInsertsAndForeignKeysAreEnforced() = runBlocking {
        val db = open()
        val sqlite = db.openHelper.writableDatabase
        val id = UUID.randomUUID().toString()
        sqlite.execSQL(
            "INSERT INTO accounts(id, name, kind, space, createdAt, updatedAt) VALUES (?, ?, 'CASH', 'PERSONAL', 10, 10)",
            arrayOf(id, "Synthetic cash"),
        )
        val account = db.accountDao().findById(id)!!
        assertEquals("IRR", account.currency)
        assertFalse(account.archived)
        assertTrue(account.includedInSpendable)
        expectFailure {
            sqlite.execSQL(
                "INSERT INTO transactions(id, accountId, amountRial, direction, kind, occurredAt, parserVersion, userEditedFields, decisionState, confidence, createdAt, updatedAt) VALUES (?, 'missing', 1, 'DEBIT', 'EXPENSE', 1, 1, '[]', 'CERTAIN', 1, 1, 1)",
                arrayOf(UUID.randomUUID().toString()),
            )
        }
    }

    @Test
    fun transactionsAreOrderedAndManualEditSetRoundTripsWithoutAmbiguousDelimiters() = runBlocking {
        val db = open()
        val account = account()
        db.accountDao().insert(account)
        val older = transaction(account.id).copy(occurredAt = 10)
        val newer = transaction(account.id).copy(
            occurredAt = 20,
            amountRial = Long.MAX_VALUE,
            userEditedFields = setOf("note", "categoryId", "a,b", "quote\"field"),
        )
        db.transactionDao().insert(older)
        db.transactionDao().insert(newer)
        assertEquals(listOf(newer, older), db.transactionDao().listForAccount(account.id))
        assertEquals(listOf(newer, older), db.transactionDao().observeForAccount(account.id).first())
        assertEquals(newer, db.transactionDao().findById(newer.id))
        val updated = newer.copy(note = "Synthetic edit", updatedAt = 30)
        db.transactionDao().update(updated)
        assertEquals(updated, db.transactionDao().listAll().first())
        db.transactionDao().delete(older)
        assertEquals(listOf(updated), db.transactionDao().observeAll().first())
    }

    @Test
    fun negativeTransactionAmountsAreRejectedWithoutMoneyConversion() {
        expectFailure { transaction(UUID.randomUUID().toString()).copy(amountRial = -1) }
    }

    @Test
    fun categoryHierarchyAndOptionalTransactionCategoryAreStored() = runBlocking {
        val db = open()
        val parent = CategoryEntity(name = "Synthetic parent", icon = "folder", color = 0xff112233, createdAt = 1, updatedAt = 1)
        val child = parent.copy(id = UUID.randomUUID().toString(), name = "Synthetic child", parentId = parent.id)
        db.categoryDao().insert(parent)
        db.categoryDao().insert(child)
        assertEquals(child, db.categoryDao().findById(child.id))
        assertEquals(2, db.categoryDao().observeAll().first().size)
        assertEquals(2, db.categoryDao().listAll().size)
        val account = account()
        db.accountDao().insert(account)
        val tx = transaction(account.id).copy(categoryId = child.id)
        db.transactionDao().insert(tx)
        db.categoryDao().delete(child)
        assertNull(db.transactionDao().findById(tx.id)!!.categoryId)
        db.categoryDao().update(parent.copy(name = "Updated synthetic parent"))
        assertEquals("Updated synthetic parent", db.categoryDao().listAll().single().name)
    }

    @Test
    fun rawReviewQueueIsOrderedAndSourceHashesAreUnique() = runBlocking {
        val db = open()
        val review = rawMessage().copy(resultType = RawMessageResult.NEEDS_REVIEW, receivedAt = 10)
        val later = rawMessage().copy(resultType = RawMessageResult.NEEDS_REVIEW, receivedAt = 20)
        val ignored = rawMessage().copy(resultType = RawMessageResult.IGNORED)
        db.rawMessageDao().insert(later)
        db.rawMessageDao().insert(review)
        db.rawMessageDao().insert(ignored)
        assertEquals(listOf(review, later), db.rawMessageDao().listReviewQueue())
        assertEquals(listOf(review, later), db.rawMessageDao().observeReviewQueue().first())
        assertEquals(review, db.rawMessageDao().findBySourceHash(review.sourceHash))
        assertEquals(review, db.rawMessageDao().findById(review.id))
        expectSuspendFailure { db.rawMessageDao().insert(review.copy(id = UUID.randomUUID().toString())) }
        val resolved = review.copy(resultType = RawMessageResult.TRANSACTION, updatedAt = 30)
        db.rawMessageDao().update(resolved)
        assertEquals(listOf(later), db.rawMessageDao().listReviewQueue())
        db.rawMessageDao().delete(ignored)
        assertEquals(2, db.rawMessageDao().listAll().size)
        assertEquals(2, db.rawMessageDao().observeAll().first().size)
    }

    @Test
    fun exportedVersionOneSchemaCanCreateARealEncryptedDatabase() = runBlocking {
        System.loadLibrary("sqlcipher")
        migrationHelper.createDatabase(name, 1).close()
        val account = account()
        open().accountDao().insert(account)
        assertEquals(account, database!!.accountDao().findById(account.id))
        // Version 1 is the baseline. No upgrade path exists or is claimed to have been tested.
    }

    private fun account() = AccountEntity(name = "Synthetic account", kind = AccountKind.BANK, space = AccountSpace.PERSONAL, createdAt = 100, updatedAt = 100)
    private fun transaction(accountId: String) = TransactionEntity(accountId = accountId, amountRial = 123, direction = TransactionDirection.DEBIT, kind = TransactionKind.EXPENSE, occurredAt = 100, parserVersion = 1, decisionState = DecisionState.CERTAIN, confidence = 1.0, createdAt = 100, updatedAt = 100)
    private fun rawMessage() = RawMessageEntity(sender = "SYNTHETIC_BANK", body = "Synthetic banking fixture", receivedAt = 100, resultType = RawMessageResult.TRANSACTION, parserVersion = 1, sourceHash = UUID.randomUUID().toString(), createdAt = 100, updatedAt = 100)
}

internal fun expectFailure(block: () -> Unit) {
    val failure = runCatching(block).exceptionOrNull()
    assertNotNull("Expected the operation to fail", failure)
}

private suspend fun expectSuspendFailure(block: suspend () -> Unit) {
    val failure = runCatching { block() }.exceptionOrNull()
    assertNotNull("Expected the operation to fail", failure)
}
