package ir.sarejam.engine.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JalaliTest {
    @Test fun knownDates() {
        assertEquals(Triple(1405, 8, 12), Jalali.fromGregorian(2026, 11, 3))
        assertEquals(Triple(1405, 1, 1), Jalali.fromGregorian(2026, 3, 21))
        assertEquals(Triple(2026, 11, 3), Jalali.toGregorian(1405, 8, 12))
    }

    @Test fun roundTripEveryDayFor40Years() {
        var d = java.time.LocalDate.of(2000, 1, 1)
        while (d.year < 2040) {
            val (jy, jm, jd) = Jalali.fromGregorian(d.year, d.monthValue, d.dayOfMonth)
            assertEquals(Triple(d.year, d.monthValue, d.dayOfMonth), Jalali.toGregorian(jy, jm, jd), "$d")
            d = d.plusDays(1)
        }
    }
}

class LinkerTest {
    private val parser = SmsParser()
    private fun t(id: String, s: Fixtures.Sms): TxnRef {
        val r = parser.parse(s.sender, s.body, Jalali.toEpoch(s.received)) as ParseResult.Transaction
        return TxnRef(id, r.bankId + ":" + (r.account ?: "-"), r.direction, r.amountRial, r.epochMillis)
    }

    @Test fun otpGivesDestinationCardAndFee() {
        val otp = parser.parse(Fixtures.shahrOtp.sender, Fixtures.shahrOtp.body, Jalali.toEpoch(Fixtures.shahrOtp.received)) as ParseResult.Otp
        val links = Linker.linkOtps(
            listOf(OtpRef(otp.bankId, otp.amountRial, otp.counterpartyCard, Jalali.toEpoch(JalaliDateTime(1405, 6, 22, 18, 57, 55)))),
            listOf(t("shahr", Fixtures.shahrDebit)),
        )
        assertEquals(listOf(OtpLink("shahr", "600000*1234", 11_000)), links)
    }

    @Test fun separateFeeSmsIsLinkedToItsTransfer() {
        val links = Linker.findFees(listOf(t("fee", Fixtures.resalatFee), t("main", Fixtures.resalatDebit), t("sms", Fixtures.resalatSmsFee)))
        assertEquals(listOf(FeeLink("fee", "main")), links)
    }

    @Test fun moneyPassingThroughOneAccountIsDetected() {
        val mellat = Linker.findPassThrough(listOf(t("in", Fixtures.mellatCredit), t("out", Fixtures.mellatDebit)))
        assertEquals(listOf(PassThrough("in", "out", 11_000)), mellat)
        val parsian = Linker.findPassThrough(listOf(t("in", Fixtures.parsianCredit), t("out", Fixtures.parsianDebit)))
        assertEquals(1, parsian.size)
    }

    @Test fun transferBetweenOwnAccounts() {
        val out = TxnRef("out", "mellat:1", Direction.DEBIT, 5_011_000, 0)
        val inn = TxnRef("in", "blu:-", Direction.CREDIT, 5_000_000, 60_000)
        assertEquals(listOf(TransferLink("out", "in", 11_000)), Linker.findInternalTransfers(listOf(out, inn)))
        assertTrue(Linker.findInternalTransfers(listOf(out, inn.copy(accountKey = "mellat:1"))).isEmpty(), "same account is not a transfer")
    }
}
