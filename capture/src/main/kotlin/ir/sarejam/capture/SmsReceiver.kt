package ir.sarejam.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Telephony
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import ir.sarejam.core.database.SarejamDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Wakes only when Android delivers an SMS; does its work in well under a second and finishes
 * (CLAUDE.md rule 2). No service, no wake lock. Logs only the outcome type and time, never text.
 */
@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    @Inject lateinit var db: SarejamDatabase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val started = SystemClock.elapsedRealtime()
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)?.filterNotNull().orEmpty()
        if (parts.isEmpty()) return
        // A long SMS arrives in parts; join them per sender.
        val messages = parts.groupBy { it.displayOriginatingAddress.orEmpty() }.map { (sender, list) ->
            Triple(sender, list.joinToString("") { it.displayMessageBody.orEmpty() }, list.minOf { it.timestampMillis })
        }
        val sim = intent.extras?.let { e ->
            listOf("android.telephony.extra.SUBSCRIPTION_INDEX", "subscription").firstNotNullOfOrNull { k ->
                if (e.containsKey(k)) e.getInt(k, -1).takeIf { it >= 0 } else null
            }
        }
        val pending = goAsync()
        scope.launch {
            try {
                withTimeoutOrNull(8_000) {
                    val ingest = SmsIngest(db)
                    for ((sender, body, at) in messages) {
                        val outcome = runCatching { ingest.ingest(sender, body, at, sim) }
                            .getOrElse { e -> Log.e(TAG, "ingest failed: ${e.javaClass.simpleName}"); null }
                        Log.i(TAG, "sms ${outcome ?: "ERROR"} in ${SystemClock.elapsedRealtime() - started}ms")
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "SarejamSms"
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
