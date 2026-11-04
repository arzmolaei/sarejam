package ir.sarejam.capture

import androidx.room.withTransaction
import ir.sarejam.core.database.AccountEntity
import ir.sarejam.core.database.RawMessageEntity
import ir.sarejam.core.database.SarejamDatabase
import ir.sarejam.core.database.SourceEntity
import ir.sarejam.core.database.TransactionEntity
import ir.sarejam.core.database.TransactionSourceEntity
import ir.sarejam.core.model.AccountKind
import ir.sarejam.core.model.Decision
import ir.sarejam.core.model.MoneyDirection
import ir.sarejam.core.model.SourceType
import ir.sarejam.core.model.TransactionKind
import ir.sarejam.engine.parser.Direction
import ir.sarejam.engine.parser.Normalizer
import ir.sarejam.engine.parser.ParseResult
import ir.sarejam.engine.parser.SmsParser
import java.security.MessageDigest

/** What happened to one incoming message. Logged by type only — never its text. */
enum class IngestOutcome { NOT_BANK, OTP, IGNORED, NEEDS_REVIEW, TRANSACTION, DUPLICATE }

/**
 * One SMS → parser → database (docs/ARCHITECTURE.md «جریان یک پیامک»).
 * - NotBank: nothing is stored (rule 4).
 * - Otp: nothing of the text is stored (rule 3); linking OTP hints is ROADMAP 1.5.
 * - Ignored / NeedsReview: kept as RawMessage so no bank message is silently lost (rule 5).
 * - Transaction: Source (dedup by text hash) + account + transaction + RawMessage.
 */
class SmsIngest(
    private val db: SarejamDatabase,
    private val parser: SmsParser = SmsParser(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun ingest(sender: String, body: String, receivedAt: Long, subscriptionId: Int?): IngestOutcome {
        val result = parser.parse(sender, body, receivedAt)
        if (result is ParseResult.NotBank) return IngestOutcome.NOT_BANK
        if (result is ParseResult.Otp) return IngestOutcome.OTP

        val hash = textHash(sender, body)
        val now = clock()
        return db.withTransaction {
            if (db.sources().byHash(hash) != null || db.rawMessages().byHash(hash) != null) return@withTransaction IngestOutcome.DUPLICATE
            val raw = RawMessageEntity(
                sender = sender, body = body, textHash = hash, receivedAt = receivedAt,
                parseResult = when (result) {
                    is ParseResult.Transaction -> "TRANSACTION"
                    is ParseResult.Ignored -> "IGNORED"
                    else -> "NEEDS_REVIEW"
                },
                parserVersion = PARSER_VERSION, createdAt = now, subscriptionId = subscriptionId,
            )
            db.rawMessages().insert(raw)
            when (result) {
                is ParseResult.Ignored -> IngestOutcome.IGNORED
                is ParseResult.Transaction -> { saveTransaction(result, sender, hash, receivedAt, subscriptionId, now); IngestOutcome.TRANSACTION }
                else -> IngestOutcome.NEEDS_REVIEW
            }
        }
    }

    private suspend fun saveTransaction(t: ParseResult.Transaction, sender: String, hash: String, receivedAt: Long, sim: Int?, now: Long) {
        val bank = t.bankId ?: sender
        val last4 = t.account?.filter(Char::isDigit)?.takeLast(4)?.takeIf { it.isNotEmpty() }
        val account = db.accounts().find(bank, last4)
            ?: db.accounts().insert(
                AccountEntity(kind = AccountKind.BANK, bank = bank, name = bank, last4 = last4, lastBalance = t.balanceRial, createdAt = now, updatedAt = now),
            ).let { id -> db.accounts().byId(id)!! }
        if (t.balanceRial != null) db.accounts().updateBalance(account.id, t.balanceRial, now)

        val sourceId = db.sources().insert(
            SourceEntity(type = SourceType.SMS, sender = sender, subscriptionId = sim, textHash = hash, receivedAt = receivedAt, notificationKey = null, accountId = account.id, createdAt = now, updatedAt = now),
        )
        val kind = when {
            t.kindHint == "failed" -> TransactionKind.FAILED
            t.kindHint == "refund" -> TransactionKind.REFUND
            t.direction == Direction.CREDIT -> TransactionKind.INCOME
            else -> TransactionKind.EXPENSE
        }
        val confidence = (t.confidence * 100).toInt().coerceIn(0, 100)
        val txId = db.transactions().insert(
            TransactionEntity(
                accountId = account.id, amount = t.amountRial,
                direction = if (t.direction == Direction.CREDIT) MoneyDirection.IN else MoneyDirection.OUT,
                occurredAt = t.epochMillis, balanceAfter = t.balanceRial, kind = kind, categoryId = null,
                counterparty = t.counterpartyCard, note = t.description, parserVersion = PARSER_VERSION,
                createdAt = now, updatedAt = now,
                decision = if (confidence >= 90) Decision.CERTAIN else Decision.SUGGESTION,
                reason = "خوانده‌شده از پیامک ${t.bankId ?: sender}", confidence = confidence,
            ),
        )
        db.sources().link(TransactionSourceEntity(txId, sourceId))
    }

    companion object {
        const val PARSER_VERSION = 1

        /** Same message twice (e.g. re-delivered or re-scanned) has the same hash. */
        fun textHash(sender: String, body: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest("$sender\n${Normalizer.normalize(body)}".toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
