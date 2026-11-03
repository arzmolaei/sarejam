package ir.sarejam.engine.parser

/**
 * Brings every bank's text to one canonical form:
 * Persian/Arabic digits → ASCII, Arabic ي/ك → Persian ی/ک, bidi marks removed,
 * Persian thousands separator → ',', unicode minus → '-', spaces collapsed, lines trimmed.
 * ZWNJ (نیم‌فاصله) is kept; regexes treat it like an optional space.
 */
object Normalizer {
    private val bidiMarks = Regex("[‎‏‪-‮⁦-⁩؜]")
    private val spaces = Regex("[ \t   ]+")

    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            sb.append(
                when (c) {
                    in '۰'..'۹' -> '0' + (c - '۰')
                    in '٠'..'٩' -> '0' + (c - '٠')
                    'ي', 'ى' -> 'ی'
                    'ك' -> 'ک'
                    'ة' -> 'ه'
                    '٬' -> ','
                    '٫' -> '.'
                    '−', '–', '—' -> '-'
                    '\r' -> '\n'
                    else -> c
                }
            )
        }
        return sb.toString()
            .replace(bidiMarks, "")
            .lines()
            .map { it.replace(spaces, " ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    /** "1,811,000" → 1811000. Rejects malformed groupings so account numbers are not read as amounts. */
    fun parseAmount(raw: String): Long? {
        val s = raw.trim().trimStart('+', '-')
        if (s.isEmpty()) return null
        if (s.contains(',')) {
            if (!Regex("""\d{1,3}(,\d{3})+""").matches(s)) return null
        } else if (!Regex("""\d{1,13}""").matches(s)) return null
        return s.replace(",", "").toLongOrNull()
    }
}
