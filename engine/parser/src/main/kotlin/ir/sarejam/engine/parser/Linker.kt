package ir.sarejam.engine.parser

/** Minimal view of a stored transaction, used to find relations between messages. */
data class TxnRef(
    val id: String,
    val accountKey: String,
    val direction: Direction,
    val amountRial: Long,
    val epochMillis: Long,
)

/** OTP hint kept after the code is discarded. */
data class OtpRef(val bankId: String?, val amountRial: Long?, val counterpartyCard: String?, val epochMillis: Long)

data class OtpLink(val txnId: String, val counterpartyCard: String?, val feeRial: Long)
data class FeeLink(val feeTxnId: String, val mainTxnId: String)
data class TransferLink(val debitTxnId: String, val creditTxnId: String, val feeRial: Long)
data class PassThrough(val creditTxnId: String, val debitTxnId: String, val feeRial: Long)

/**
 * Finds relations the user would otherwise have to explain by hand.
 * All windows and limits are deliberately conservative: a missed link only means one more question,
 * a wrong link would silently corrupt the reports.
 */
object Linker {
    private const val MINUTE = 60_000L
    const val MAX_FEE_RIAL = 1_000_000L

    /** OTP («انتقال به … مبلغ … رمز») followed within 5 minutes by a debit of amount + small fee. */
    fun linkOtps(otps: List<OtpRef>, txns: List<TxnRef>): List<OtpLink> = otps.mapNotNull { otp ->
        val amount = otp.amountRial ?: return@mapNotNull null
        txns.filter {
            it.direction == Direction.DEBIT &&
                it.epochMillis in otp.epochMillis..(otp.epochMillis + 5 * MINUTE) &&
                it.amountRial - amount in 0..MAX_FEE_RIAL
        }.minByOrNull { it.epochMillis }
            ?.let { OtpLink(it.id, otp.counterpartyCard, it.amountRial - amount) }
    }

    /** A small debit in the same account within 2 minutes of a large one is that transfer's fee. */
    fun findFees(txns: List<TxnRef>): List<FeeLink> {
        val debits = txns.filter { it.direction == Direction.DEBIT }
        return debits.mapNotNull { small ->
            if (small.amountRial > MAX_FEE_RIAL) return@mapNotNull null
            debits.filter {
                it.id != small.id && it.accountKey == small.accountKey &&
                    kotlin.math.abs(it.epochMillis - small.epochMillis) <= 2 * MINUTE &&
                    it.amountRial >= small.amountRial * 20
            }.maxByOrNull { it.amountRial }?.let { FeeLink(small.id, it.id) }
        }
    }

    /** Debit in one of the user's accounts and a matching credit in ANOTHER of them = moving own money. */
    fun findInternalTransfers(txns: List<TxnRef>, windowMinutes: Long = 180): List<TransferLink> {
        val used = mutableSetOf<String>()
        return txns.filter { it.direction == Direction.DEBIT }.sortedBy { it.epochMillis }.mapNotNull { d ->
            txns.filter { c ->
                c.direction == Direction.CREDIT && c.accountKey != d.accountKey && c.id !in used &&
                    kotlin.math.abs(c.epochMillis - d.epochMillis) <= windowMinutes * MINUTE &&
                    d.amountRial - c.amountRial in 0..MAX_FEE_RIAL
            }.minByOrNull { kotlin.math.abs(it.epochMillis - d.epochMillis) }
                ?.also { used += it.id }
                ?.let { TransferLink(d.id, it.id, d.amountRial - it.amountRial) }
        }
    }

    /** Money that arrived and left the SAME account soon after (≈ same amount): ask «فقط رد شد؟». */
    fun findPassThrough(txns: List<TxnRef>, windowMinutes: Long = 240): List<PassThrough> =
        txns.filter { it.direction == Direction.CREDIT }.mapNotNull { c ->
            txns.filter { d ->
                d.direction == Direction.DEBIT && d.accountKey == c.accountKey &&
                    d.epochMillis - c.epochMillis in 0..(windowMinutes * MINUTE) &&
                    d.amountRial - c.amountRial in 0..MAX_FEE_RIAL
            }.minByOrNull { it.epochMillis }?.let { PassThrough(c.id, it.id, it.amountRial - c.amountRial) }
        }
}
