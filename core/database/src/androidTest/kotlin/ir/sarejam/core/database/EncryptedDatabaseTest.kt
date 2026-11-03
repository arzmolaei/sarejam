package ir.sarejam.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "test-encrypted.db"
    private val key = DatabaseKey(context, alias = "test_db_key")
    private lateinit var dbFile: File

    @Before fun setUp() {
        context.deleteDatabase(name)
        dbFile = context.getDatabasePath(name)
    }

    @After fun tearDown() {
        context.deleteDatabase(name)
    }

    private suspend fun writeOneTransaction(): Long {
        val db = DatabaseFactory.open(context, key, name)
        val now = 1_700_000_000_000
        val account = db.accounts().insert(
            AccountEntity(kind = "BANK", bank = "TEST", name = "حساب آزمایشی", last4 = "0000", lastBalance = 5_000_000, createdAt = now, updatedAt = now),
        )
        db.transactions().insert(
            TransactionEntity(
                accountId = account, amount = 1_250_000, direction = "OUT", occurredAt = now, balanceAfter = 3_750_000,
                kind = "EXPENSE", categoryId = null, counterparty = null, note = null, createdAt = now, updatedAt = now,
            ),
        )
        db.close()
        return account
    }

    @Test fun dataSurvivesReopeningWithTheSameKey() = runTest {
        val account = writeOneTransaction()
        val db = DatabaseFactory.open(context, key, name)
        val rows = db.transactions().forAccount(account)
        db.close()
        assertEquals(1, rows.size)
        assertEquals(1_250_000L, rows.single().amount)
    }

    @Test fun fileIsNotReadableWithoutTheKey() = runTest {
        writeOneTransaction()
        // A plain SQLite file starts with "SQLite format 3"; an encrypted one must not.
        val header = dbFile.inputStream().use { it.readNBytes(16) }
        assertFalse(String(header, Charsets.US_ASCII).startsWith("SQLite format 3"))
        try {
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { it.rawQuery("SELECT * FROM account", null).use { c -> c.count } }
            fail("plain SQLite opened the encrypted database")
        } catch (expected: Exception) {
            // "file is not a database"
        }
    }

    @Test fun fileIsNotReadableWithAWrongKey() = runTest {
        writeOneTransaction()
        System.loadLibrary("sqlcipher")
        try {
            net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
                dbFile.path, ByteArray(32) { 7 }, null, net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY, null,
            ).use { it.rawQuery("SELECT count(*) FROM account", null).use { c -> c.moveToFirst() } }
            fail("wrong key opened the database")
        } catch (expected: Exception) {
        }
    }

    @Test fun passphraseIsStableAndRandom() {
        val first = key.passphrase()
        assertArrayEquals(first, DatabaseKey(context, alias = "test_db_key").passphrase())
        assertEquals(32, first.size)
        assertFalse(first.contentEquals(DatabaseKey(context, alias = "test_db_key_other").passphrase()))
    }
}
