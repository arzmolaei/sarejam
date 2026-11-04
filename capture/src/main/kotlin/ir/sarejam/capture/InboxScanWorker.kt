package ir.sarejam.capture

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import ir.sarejam.core.database.SarejamDatabase
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CaptureEntryPoint {
    fun database(): SarejamDatabase
}

/**
 * Reads the inbox in the background (never inside the SMS receiver). First run: the last 6 months;
 * later runs: only since the previous successful scan. Progress: KEY_DONE / KEY_TOTAL.
 */
class InboxScanWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val since = prefs.getLong(LAST_SCAN, now - SIX_MONTHS)
        val db = EntryPointAccessors.fromApplication(applicationContext, CaptureEntryPoint::class.java).database()
        return runCatching {
            val summary = InboxScan(SystemInboxReader(applicationContext), SmsIngest(db)).run(since) { done, total ->
                setProgress(workDataOf(KEY_DONE to done, KEY_TOTAL to total))
            }
            // Overlap a little so a message delivered during the scan is never missed; dedup handles repeats.
            prefs.edit().putLong(LAST_SCAN, now - TimeUnit.HOURS.toMillis(1)).apply()
            Log.i(TAG, "scan read=${summary.read} tx=${summary.transactions} review=${summary.needsReview}")
            Result.success(workDataOf(KEY_DONE to summary.read, KEY_TOTAL to summary.read, KEY_TRANSACTIONS to summary.transactions))
        }.getOrElse { e ->
            Log.e(TAG, "scan failed: ${e.javaClass.simpleName}")
            if (e is SecurityException) Result.failure() else Result.retry()
        }
    }

    companion object {
        const val UNIQUE = "inbox-scan"
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"
        const val KEY_TRANSACTIONS = "transactions"
        private const val TAG = "SarejamScan"
        private const val PREFS = "capture"
        private const val LAST_SCAN = "last_inbox_scan"
        private val SIX_MONTHS = TimeUnit.DAYS.toMillis(183)

        /** Safe to call on every app open: one scan at a time, and only new messages are read. */
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<InboxScanWorker>()
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE, ExistingWorkPolicy.KEEP, request)
        }
    }
}
