package ir.sarejam.capture

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ir.sarejam.core.database.SarejamDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InboxScanTest {
    private val blu = "+9890000258"
    private fun debit(amount: String, day: Int) =
        "بلو\nبرداشت پول\nکاربر عزیز، $amount ریال از\nحساب شما پرید.\nموجودی: 17,369,366 ریال\n۷:۴۳\n۱۴۰۵.۰۷.۰$day"

    @Test fun scanImportsBankMessagesSkipsPersonalAndRepeatsAndReportsProgress() = runTest {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), SarejamDatabase::class.java).build()
        val inbox = listOf(
            InboxSms(blu, debit("400,000", 1), 1_000, 1),
            InboxSms("09121234567", "سلام، فردا میای؟", 2_000, 1),
            InboxSms(blu, debit("800,000", 2), 3_000, 1),
        )
        val ingest = SmsIngest(db) { 1 }
        // The first message already came in live.
        ingest.ingest(inbox[0].sender, inbox[0].body, inbox[0].receivedAt, 1)
        val progress = mutableListOf<Pair<Int, Int>>()
        val summary = InboxScan({ inbox }, ingest).run(0) { d, t -> progress += d to t }
        assertEquals(ScanSummary(read = 3, transactions = 1, needsReview = 0), summary)
        assertEquals(2, db.transactions().count())
        assertEquals(3 to 3, progress.last())
        db.close()
    }
}
