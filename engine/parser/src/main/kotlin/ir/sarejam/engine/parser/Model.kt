package ir.sarejam.engine.parser

/** Direction of money relative to the user's account. */
enum class Direction { CREDIT, DEBIT }

/** Why a bank message was intentionally not turned into a transaction. */
enum class IgnoreReason { LOGIN, PROMO, INFO }

/** A Jalali (Shamsi) date-time as read from the message. Year may be missing in the text. */
data class JalaliDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int = 0,
)

sealed interface ParseResult {
    /** Personal / non-bank SMS. Must never be stored. */
    data object NotBank : ParseResult

    /** A real money movement. */
    data class Transaction(
        val bankId: String?,
        val direction: Direction,
        val amountRial: Long,
        val balanceRial: Long?,
        val account: String?,
        val counterpartyCard: String?,
        val dateTime: JalaliDateTime,
        val epochMillis: Long,
        val description: String?,
        val categoryHint: String?,
        /** "refund" (عودت/برگشت وجه) or "failed" (ناموفق) — the app maps these to Transaction kinds. */
        val kindHint: String?,
        val confidence: Double,
        val parser: String,
    ) : ParseResult

    /**
     * One-time-password message. Only the non-secret hints are kept (amount and destination card);
     * the code itself is never extracted, returned or stored.
     */
    data class Otp(
        val bankId: String?,
        val amountRial: Long?,
        val counterpartyCard: String?,
        val epochMillis: Long,
    ) : ParseResult

    /** Bank message that is not a transaction (login notice, greeting, service notice). */
    data class Ignored(val bankId: String?, val reason: IgnoreReason) : ParseResult

    /** Looks like a bank transaction but could not be read with confidence → review queue. */
    data class NeedsReview(val bankId: String?, val reason: String) : ParseResult
}
