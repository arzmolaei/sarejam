package ir.sarejam.core.ui

internal fun isAutomaticDarkHour(hour: Int): Boolean = hour >= 18 || hour < 6

/** Work with characters rather than abs(Long) so Long.MIN_VALUE remains exact. */
internal fun formatSignedAmount(amountRial: Long, usePersianDigits: Boolean): String {
    val digits = amountRial.toString().removePrefix("-")
    val separator = if (usePersianDigits) '٬' else ','
    val grouped = digits.reversed().chunked(3).joinToString(separator.toString()).reversed()
    val number = if (usePersianDigits) grouped.map {
        if (it in '0'..'9') ('۰'.code + (it - '0')).toChar() else it
    }.joinToString("") else grouped
    val sign = when {
        amountRial > 0 -> "+"
        amountRial < 0 -> "−"
        else -> ""
    }
    return sign + number
}
