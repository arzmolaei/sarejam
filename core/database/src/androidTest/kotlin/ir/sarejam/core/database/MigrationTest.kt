package ir.sarejam.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Data must survive every update (CLAUDE.md rule 7). For each new version:
 * add the Migration to SarejamDatabase.MIGRATIONS, keep the exported schema, and this test
 * walks a database created at version 1 with real rows through every migration to the latest.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), SarejamDatabase::class.java)

    private val name = "migration-test.db"

    @Test
    fun everyMigrationFromVersionOneKeepsData() {
        helper.createDatabase(name, 1).use { db ->
            db.execSQL(
                "INSERT INTO account (id, kind, bank, name, last4, space, currency, lastBalance, archived, createdAt, updatedAt) " +
                    "VALUES (1, 'BANK', 'TEST', 'test', '0000', 'PERSONAL', 'IRR', 100, 0, 1, 1)",
            )
            db.execSQL(
                "INSERT INTO `transaction` (id, accountId, amount, direction, occurredAt, balanceAfter, kind, parserVersion, createdAt, updatedAt) " +
                    "VALUES (1, 1, 50, 'OUT', 1, 50, 'EXPENSE', 1, 1, 1)",
            )
        }
        val db = helper.runMigrationsAndValidate(name, SarejamDatabase.VERSION, true, *SarejamDatabase.MIGRATIONS)
        db.query("SELECT amount FROM `transaction` WHERE id = 1").use { c ->
            check(c.moveToFirst() && c.getLong(0) == 50L) { "transaction lost during migration" }
        }
        db.close()
    }
}
