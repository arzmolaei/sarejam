package ir.sarejam.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.sarejam.core.model.AccountKind
import ir.sarejam.core.model.EditableField
import ir.sarejam.core.model.MoneyDirection
import ir.sarejam.core.model.PeriodTotals
import ir.sarejam.core.model.TransactionKind
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionEditsTest {
    private lateinit var db: SarejamDatabase
    private lateinit var edits: TransactionEdits
    private var account = 0L

    @Before fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), SarejamDatabase::class.java).build()
        edits = TransactionEdits(db) { 42 }
        account = db.accounts().insert(AccountEntity(kind = AccountKind.BANK, bank = "TEST", name = "test", last4 = null, lastBalance = null, createdAt = 0, updatedAt = 0))
    }

    @After fun tearDown() = db.close()

    private suspend fun add(kind: TransactionKind, amount: Long, at: Long = 100) = db.transactions().insert(
        TransactionEntity(
            accountId = account, amount = amount, direction = MoneyDirection.OUT, occurredAt = at, balanceAfter = null,
            kind = kind, categoryId = null, counterparty = null, note = null, createdAt = 0, updatedAt = 0,
        ),
    )

    @Test fun reparseDoesNotOverwriteWhatTheUserFixed() = runTest {
        val id = add(TransactionKind.EXPENSE, 1_250_000)
        edits.userEdit(id) { it.copy(amount = 1_200_000, note = "نان و شیر") }
        val fresh = db.transactions().byId(id)!!.fields().copy(amount = 1_250_000, note = null, counterparty = "6037")
        val changed = edits.reparse(id, fresh, parserVersion = 2)
        val row = db.transactions().byId(id)!!
        assertEquals(1_200_000L, row.amount)
        assertEquals("نان و شیر", row.note)
        assertEquals("6037", row.counterparty)
        assertEquals(setOf(EditableField.AMOUNT, EditableField.NOTE), row.userEditedFields)
        assertEquals(setOf(EditableField.COUNTERPARTY), changed)
        assertEquals(2, row.parserVersion)
    }

    @Test fun transferAndAdjustmentAreNotInTheExpenseReport() = runTest {
        add(TransactionKind.EXPENSE, 1_000_000)
        add(TransactionKind.FEE, 11_000)
        add(TransactionKind.TRANSFER, 5_000_000)
        add(TransactionKind.ADJUSTMENT, 700_000)
        add(TransactionKind.FAILED, 300_000)
        add(TransactionKind.EXPENSE, 999_999, at = 5_000) // outside the window
        assertEquals(PeriodTotals(expense = 1_011_000, income = 0), edits.totals(from = 0, to = 1_000))
    }
}
