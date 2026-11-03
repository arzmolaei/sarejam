package ir.sarejam.engine.parser

/**
 * What we know about one bank. Everything not listed here is handled by the generic rules in [SmsParser].
 * (Later these move to a JSON file so new banks ship without an app update.)
 */
data class BankProfile(
    val id: String,
    val nameFa: String,
    /** Sender ids, compared case-insensitively after removing spaces, '+', '-' and leading 98/0. */
    val senders: List<String>,
    /** Words in the body that identify the bank when the sender is a plain number. */
    val bodyMarkers: List<String> = emptyList(),
    /** Card BIN prefixes (first 6 digits). */
    val bins: List<String> = emptyList(),
    /** The bank puts a free-text description line in transaction messages (e.g. «کارمزد پیامک …»). */
    val hasDescriptionLine: Boolean = false,
)

object BankProfiles {
    val all: List<BankProfile> = listOf(
        BankProfile("parsian", "پارسیان", listOf("parsianbank", "parsian"), listOf("بانک پارسیان"), listOf("622106", "627884", "639194")),
        BankProfile("blu", "بلو", listOf("90000258"), listOf("بلو"), emptyList()),
        BankProfile("mellat", "ملت", listOf("bankmellat", "mellat"), listOf("بانک ملت"), listOf("610433", "991975")),
        BankProfile("resalat", "رسالت", listOf("resalatbank", "resalat"), listOf("رسالت"), listOf("504172"), hasDescriptionLine = true),
        BankProfile("shahr", "شهر", listOf("bankshahr", "shahrbank"), listOf("بانک شهر"), listOf("502806", "504706")),
        BankProfile("melli", "ملی", listOf("bankmelli", "melli"), listOf("بانک ملی"), listOf("603799")),
        BankProfile("saderat", "صادرات", listOf("banksaderat", "saderat"), listOf("بانک صادرات"), listOf("603769")),
        BankProfile("tejarat", "تجارت", listOf("tejaratbank", "tejarat"), listOf("بانک تجارت"), listOf("627353", "585983")),
        BankProfile("sepah", "سپه", listOf("banksepah", "sepah"), listOf("بانک سپه"), listOf("589210")),
        BankProfile("refah", "رفاه", listOf("refahbank", "refah"), listOf("بانک رفاه"), listOf("589463")),
        BankProfile("keshavarzi", "کشاورزی", listOf("bki", "keshavarzi"), listOf("بانک کشاورزی"), listOf("603770", "639217")),
        BankProfile("maskan", "مسکن", listOf("bankmaskan", "maskan"), listOf("بانک مسکن"), listOf("628023")),
        BankProfile("pasargad", "پاسارگاد", listOf("bpi", "pasargad"), listOf("پاسارگاد"), listOf("502229", "639347")),
        BankProfile("saman", "سامان", listOf("sb24", "samanbank", "saman"), listOf("بانک سامان"), listOf("621986")),
        BankProfile("eghtesadnovin", "اقتصاد نوین", listOf("enbank", "eghtesadnovin"), listOf("اقتصاد نوین"), listOf("627412")),
        BankProfile("ayandeh", "آینده", listOf("ayandehbank", "ayandeh"), listOf("بانک آینده"), listOf("636214")),
        BankProfile("postbank", "پست‌بانک", listOf("postbank"), listOf("پست بانک", "پست‌بانک"), listOf("627760")),
    )

    private fun key(s: String): String {
        val k = s.lowercase().replace(Regex("[\\s+\\-_.]"), "")
        return if (k.all { it.isDigit() }) k.removePrefix("98").removePrefix("0") else k
    }

    fun bySender(sender: String): BankProfile? {
        val k = key(sender)
        return all.firstOrNull { p -> p.senders.any { key(it) == k } }
    }

    fun byBody(normalizedBody: String): BankProfile? {
        val head = normalizedBody.lineSequence().take(2).joinToString(" ")
        return all.firstOrNull { p -> p.bodyMarkers.any { head.contains(it) } }
    }

    fun byBin(card: String?): BankProfile? {
        val bin = card?.filter { it.isDigit() }?.take(6) ?: return null
        return all.firstOrNull { bin in it.bins }
    }
}
