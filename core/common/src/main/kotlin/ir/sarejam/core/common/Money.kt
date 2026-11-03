package ir.sarejam.core.common

/** Money is always whole rials in a Long (CLAUDE.md rule 8). */
object Money {
    private const val PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
    private const val SEPARATOR = '٬'

    /** 1250000 → «۱٬۲۵۰٬۰۰۰». Negative values keep their minus sign in front. */
    fun group(value: Long, persianDigits: Boolean = true): String {
        val digits = if (value == Long.MIN_VALUE) "9223372036854775808" else kotlin.math.abs(value).toString()
        val grouped = digits.reversed().chunked(3).joinToString(SEPARATOR.toString()).reversed()
        val localized = if (persianDigits) grouped.map { c -> if (c in '0'..'9') PERSIAN_DIGITS[c - '0'] else c }.joinToString("") else grouped
        return if (value < 0) "−$localized" else localized
    }

    /** Rial → toman for display only; storage stays in rial. */
    fun rialToToman(rial: Long): Long = rial / 10
}
