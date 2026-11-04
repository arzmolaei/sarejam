package ir.sarejam.capture

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.sarejam.core.database.SarejamDatabase
import ir.sarejam.core.model.MoneyDirection
import ir.sarejam.core.model.TransactionKind
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Samples are anonymized structure-only fixtures (same as engine/parser Fixtures.kt). */
@RunWith(AndroidJUnit4::class)
class SmsIngestTest {
    private lateinit var db: SarejamDatabase
    private lateinit var ingest: SmsIngest

    private val blu = "+9890000258"
    private val bluDebit = "بلو\nبرداشت پول\nکاربر عزیز، 400,000 ریال از\nحساب شما پرید.\nموجودی: 17,369,366 ریال\n۷:۴۳\n۱۴۰۵.۰۷.۰۱"
    private val bluWelcome = "بلو\nکاربر عزیز خوش آمدید.\n14:09:07\n1405.08.10"

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), SarejamDatabase::class.java).build()
        ingest = SmsIngest(db) { 1 }
    }

    @After fun tearDown() = db.close()

    private suspend fun rows() = listOf(db.transactions().count(), db.rawMessages().count())

    @Test fun bankDebitBecomesATransactionWithAccountAndBalance() = runTest {
        assertEquals(IngestOutcome.TRANSACTION, ingest.ingest(blu, bluDebit, 1_000, subscriptionId = 1))
        val tx = db.transactions().forAccount(1).single()
        assertEquals(400_000L, tx.amount)
        assertEquals(MoneyDirection.OUT, tx.direction)
        assertEquals(TransactionKind.EXPENSE, tx.kind)
        assertEquals(17_369_366L, db.accounts().byId(tx.accountId)!!.lastBalance)
    }

    @Test fun personalSmsLeavesNoTrace() = runTest {
        assertEquals(IngestOutcome.NOT_BANK, ingest.ingest("09121234567", "سلام، شام می‌آیی؟ ۵۰۰ هزار تومان قرض بده", 1_000, null))
        assertEquals(listOf(0, 0), rows())
    }

    @Test fun otpTextIsNeverStored() = runTest {
        val otp = "رمز پویا: 482913\nمبلغ: 1,800,000 ریال\nکارت مقصد 6037-99**-****-1234\nبانک شهر"
        assertEquals(IngestOutcome.OTP, ingest.ingest("Bank Shahr", otp, 1_000, null))
        assertEquals(listOf(0, 0), rows())
    }

    @Test fun sameSmsTwiceIsOneTransaction() = runTest {
        ingest.ingest(blu, bluDebit, 1_000, 1)
        assertEquals(IngestOutcome.DUPLICATE, ingest.ingest(blu, bluDebit, 1_000, 1))
        assertEquals(1, db.transactions().count())
    }

    @Test fun bankNoticeIsKeptNotLost() = runTest {
        assertEquals(IngestOutcome.IGNORED, ingest.ingest(blu, bluWelcome, 1_000, null))
        assertEquals(listOf(0, 1), rows())
    }
}
