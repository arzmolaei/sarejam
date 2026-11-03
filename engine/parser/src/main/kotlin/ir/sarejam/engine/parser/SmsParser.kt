package ir.sarejam.engine.parser

/**
 * Turns one SMS (or notification text) into a [ParseResult].
 *
 * Order matters:
 *  1. not bank-like at all → [ParseResult.NotBank] (never stored)
 *  2. one-time password → [ParseResult.Otp] (code discarded, only amount + destination card kept)
 *  3. login / greeting / notice → [ParseResult.Ignored]
 *  4. amount + direction found → [ParseResult.Transaction]
 *  5. otherwise → [ParseResult.NeedsReview] (nothing bank-like is ever silently dropped)
 */
class SmsParser(private val profiles: BankProfiles = BankProfiles) {

    fun parse(sender: String, body: String, receivedAtMillis: Long): ParseResult {
        val text = Normalizer.normalize(body)
        val lines = text.lines()
        val profile = profiles.bySender(sender) ?: profiles.byBody(text)

        // Personal mobile numbers (friends, family) are never read as banks, whatever they write.
        if (profile == null && PERSONAL_MOBILE.matches(sender.replace(" ", ""))) return ParseResult.NotBank
        if (profile == null && !BANK_HINT.containsMatchIn(text)) return ParseResult.NotBank

        if (isOtp(text)) {
            return ParseResult.Otp(
                bankId = profile?.id,
                amountRial = LABELED_AMOUNT.find(text)?.let { Normalizer.parseAmount(it.groupValues[2]) },
                counterpartyCard = MASKED_CARD.find(text)?.value,
                epochMillis = receivedAtMillis,
            )
        }

        val amount = findAmount(text, lines)
        if (amount == null) {
            if (profile == null) return ParseResult.NotBank
            return when {
                LOGIN.containsMatchIn(text) -> ParseResult.Ignored(profile?.id, IgnoreReason.LOGIN)
                profile != null && !MONEY_WORDS.containsMatchIn(text) -> ParseResult.Ignored(profile.id, IgnoreReason.PROMO)
                profile != null && !BALANCE.containsMatchIn(text) -> ParseResult.Ignored(profile.id, IgnoreReason.INFO)
                else -> ParseResult.NeedsReview(profile?.id, "amount not found")
            }
        }

        val direction = amount.sign ?: directionFromWords(text)
            ?: return ParseResult.NeedsReview(profile?.id, "direction not found")

        val balance = findBalance(lines)
        // Unknown sender: only trust it when it also reports a balance, like every real bank SMS does.
        if (profile == null && balance == null) return ParseResult.NeedsReview(null, "unknown sender without balance")
        val card = MASKED_CARD.find(text)?.value
        val received = Jalali.fromEpoch(receivedAtMillis)
        val dateTime = findDateTime(text, received) ?: received
        val description = if (profile?.hasDescriptionLine == true) findDescription(lines, profile) else null

        var confidence = 0.6
        if (amount.labeled) confidence += 0.15
        if (amount.sign != null) confidence += 0.1
        if (balance != null) confidence += 0.15
        if (profile == null) confidence -= 0.15

        return ParseResult.Transaction(
            bankId = profile?.id ?: profiles.byBin(card)?.id,
            direction = direction,
            amountRial = amount.value,
            balanceRial = balance,
            account = findAccount(lines),
            counterpartyCard = card,
            dateTime = dateTime,
            epochMillis = Jalali.toEpoch(dateTime),
            description = description,
            categoryHint = categoryHint(description ?: text),
            confidence = confidence.coerceIn(0.0, 1.0),
            parser = profile?.id ?: "generic",
        )
    }

    // ---------------------------------------------------------------- amount & direction

    private data class Amount(val value: Long, val sign: Direction?, val labeled: Boolean)

    private fun findAmount(text: String, lines: List<String>): Amount? {
        LABELED_AMOUNT.find(text)?.let { m ->
            Normalizer.parseAmount(m.groupValues[2])?.let { return Amount(it, sign(m.groupValues[1]), true) }
        }
        KEYWORD_AMOUNT.find(text)?.let { m ->
            Normalizer.parseAmount(m.groupValues[2])?.let {
                val dir = if (m.groupValues[1] == "واریز") Direction.CREDIT else Direction.DEBIT
                return Amount(it, sign(m.groupValues[2].take(1)) ?: dir, true)
            }
        }
        for (line in lines) {
            SIGNED_LINE.matchEntire(line)?.let { m ->
                Normalizer.parseAmount(m.groupValues[2])?.let { return Amount(it, sign(m.groupValues[1]), false) }
            }
        }
        for (line in lines) {
            if (BALANCE_WORD.containsMatchIn(line)) continue
            RIAL_AMOUNT.find(line)?.let { m ->
                Normalizer.parseAmount(m.groupValues[1])?.let { return Amount(it, null, false) }
            }
        }
        return null
    }

    private fun sign(s: String): Direction? = when (s) {
        "+" -> Direction.CREDIT
        "-" -> Direction.DEBIT
        else -> null
    }

    private fun directionFromWords(text: String): Direction? {
        val debit = DEBIT_WORDS.containsMatchIn(text)
        val credit = CREDIT_WORDS.containsMatchIn(text)
        return when {
            debit && !credit -> Direction.DEBIT
            credit && !debit -> Direction.CREDIT
            else -> null
        }
    }

    // ---------------------------------------------------------------- other fields

    private fun findBalance(lines: List<String>): Long? {
        for (line in lines) {
            BALANCE.find(line)?.let { m -> Normalizer.parseAmount(m.groupValues[2])?.let { return if (m.groupValues[2].startsWith("-")) -it else it } }
        }
        return null
    }

    private fun findAccount(lines: List<String>): String? {
        for (line in lines) {
            if (FULL_DATE.containsMatchIn(line)) continue
            ACCOUNT_LINE.matchEntire(line)?.let { return it.value }
        }
        for (line in lines) ACCOUNT_LABELED.find(line)?.let { return it.groupValues[2] }
        return null
    }

    private fun findDescription(lines: List<String>, profile: BankProfile): String? =
        lines
            .filter { PERSIAN_LETTER.containsMatchIn(it) && !LABEL_LINE.containsMatchIn(it) && profile.bodyMarkers.none { m -> it.contains(m) } }
            .joinToString(" ")
            .ifBlank { null }

    private fun categoryHint(text: String): String? = when {
        FEE_WORDS.containsMatchIn(text) -> "bank_fee"
        else -> null
    }

    private fun isOtp(text: String): Boolean = OTP.containsMatchIn(text)

    // ---------------------------------------------------------------- dates

    private fun findDateTime(text: String, received: JalaliDateTime): JalaliDateTime? {
        val time = TIME.find(text)
        val hour = time?.groupValues?.get(1)?.toInt() ?: received.hour
        val minute = time?.groupValues?.get(2)?.toInt() ?: received.minute
        val second = time?.groupValues?.get(3)?.takeIf { it.isNotEmpty() }?.toInt() ?: 0
        if (hour > 23 || minute > 59) return null

        val (y, m, d) = FULL_DATE.find(text)?.let { Triple(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt()) }
            ?: SHORT_YEAR_DATE.find(text)?.let { Triple(1400 + it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt()) }
            ?: MONTH_DAY.find(text)?.let {
                val mm = it.groupValues[1].toInt()
                // A message dated Esfand but received in Farvardin belongs to the previous year.
                val year = if (mm > received.month + 1) received.year - 1 else received.year
                Triple(year, mm, it.groupValues[2].toInt())
            }
            ?: return null
        if (m !in 1..12 || d !in 1..31) return null
        return JalaliDateTime(y, m, d, hour, minute, second)
    }

    companion object {
        private const val ZW = "[‌ ]?"
        private const val NUM = """[+-]?\d[\d,]*"""

        private val PERSONAL_MOBILE = Regex("""(\+?98|0)?9\d{9}""")
        private val BANK_HINT = Regex("مانده|موجودی|برداشت|واریز|مبلغ|ریال|حساب|کارت")
        private val MONEY_WORDS = Regex("مبلغ|ریال|برداشت|واریز|مانده|موجودی|انتقال")
        private val OTP = Regex("""(رمز|کد)$ZW(پویا|یک${ZW}بار${ZW}مصرف|تایید|تأیید|اعتبار)?\s*:?\s*\d{4,8}(?!\d|,)""")
        private val LOGIN = Regex("""ورود[\s\u200C]*به|خوش[\s\u200C]*آمدید|ورود[\s\u200C]*موفق""")
        private val LABELED_AMOUNT = Regex("""مبلغ\s*(?:تراکنش)?\s*:?\s*([+-]?)\s*(\d[\d,]*)""")
        private val KEYWORD_AMOUNT = Regex("""(برداشت|واریز)\s*:?\s*($NUM)""")
        private val SIGNED_LINE = Regex("""([+-])\s*(\d[\d,]*)(?:\s*ریال)?""")
        private val RIAL_AMOUNT = Regex("""(\d{1,3}(?:,\d{3})+)\s*ریال""")
        private val BALANCE_WORD = Regex("مانده|موجودی")
        private val BALANCE = Regex("""(مانده|موجودی)\s*(?:حساب)?\s*:?\s*($NUM)""")
        private val DEBIT_WORDS = Regex("برداشت|خرید|پرید|کسر|پرداخت|انتقال${ZW}از|انتقال${ZW}وجه")
        private val CREDIT_WORDS = Regex("واریز|نشست|دریافت|افزایش")
        private val MASKED_CARD = Regex("""\d{4,6}\*+\d{4}""")
        private val ACCOUNT_LINE = Regex("""\d[\d.\-]{7,}\d""")
        private val ACCOUNT_LABELED = Regex("""(حساب|از)\s*:?\s*(\d[\d.\-]{5,}\d)""")
        private val PERSIAN_LETTER = Regex("[؀-ۿ]")
        private val LABEL_LINE = Regex("مانده|موجودی|مبلغ|حساب|برداشت|واریز|تاریخ|ریال")
        private val FEE_WORDS = Regex("کارمزد")

        private val TIME = Regex("""(?<!\d)(\d{1,2}):(\d{2})(?::(\d{2}))?(?!\d)""")
        private val FULL_DATE = Regex("""(?<!\d)(1[34]\d{2})[/.\-](\d{1,2})[/.\-](\d{1,2})(?!\d)""")
        private val SHORT_YEAR_DATE = Regex("""(?<![\d/])(\d{2})/(\d{2})/(\d{2})(?![\d/])""")
        private val MONTH_DAY = Regex("""(?<![\d/.,])(\d{2})/(\d{2})(?![\d/.,])""")
    }
}
