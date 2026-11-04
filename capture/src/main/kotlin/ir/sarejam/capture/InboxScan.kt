package ir.sarejam.capture

import android.content.Context
import android.net.Uri
import android.provider.Telephony

/** One stored SMS from the phone's inbox. */
data class InboxSms(val sender: String, val body: String, val receivedAt: Long, val subscriptionId: Int?)

fun interface InboxReader {
    /** Messages received at or after [since], oldest first. */
    fun read(since: Long): List<InboxSms>
}

/** Reads the system SMS inbox (needs READ_SMS). */
class SystemInboxReader(private val context: Context) : InboxReader {
    override fun read(since: Long): List<InboxSms> {
        val out = ArrayList<InboxSms>()
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.SUBSCRIPTION_ID)
        context.contentResolver.query(
            Uri.parse("content://sms/inbox"), projection, "${Telephony.Sms.DATE} >= ?", arrayOf(since.toString()), "${Telephony.Sms.DATE} ASC",
        )?.use { c ->
            val a = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val b = c.getColumnIndex(Telephony.Sms.BODY)
            val d = c.getColumnIndex(Telephony.Sms.DATE)
            val s = c.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)
            while (c.moveToNext()) {
                val sender = c.getString(a) ?: continue
                val body = c.getString(b) ?: continue
                out += InboxSms(sender, body, c.getLong(d), if (s >= 0 && !c.isNull(s)) c.getInt(s).takeIf { it >= 0 } else null)
            }
        }
        return out
    }
}

data class ScanSummary(val read: Int, val transactions: Int, val needsReview: Int)

/** Feeds old messages through the same ingest as live SMS; duplicates are skipped by text hash. */
class InboxScan(private val reader: InboxReader, private val ingest: SmsIngest) {
    suspend fun run(since: Long, onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> }): ScanSummary {
        val messages = reader.read(since)
        var tx = 0
        var review = 0
        messages.forEachIndexed { i, m ->
            when (ingest.ingest(m.sender, m.body, m.receivedAt, m.subscriptionId)) {
                IngestOutcome.TRANSACTION -> tx++
                IngestOutcome.NEEDS_REVIEW -> review++
                else -> Unit
            }
            if (i % 20 == 19 || i == messages.lastIndex) onProgress(i + 1, messages.size)
        }
        return ScanSummary(messages.size, tx, review)
    }
}
