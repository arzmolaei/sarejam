package ir.sarejam.engine.parser

import ir.sarejam.engine.parser.Direction.CREDIT
import ir.sarejam.engine.parser.Direction.DEBIT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SmsParserTest {
    private val parser = SmsParser()

    private fun parse(s: Fixtures.Sms) = parser.parse(s.sender, s.body, Jalali.toEpoch(s.received))

    private fun txn(
        s: Fixtures.Sms, bank: String, dir: Direction, amount: Long, balance: Long?,
        date: JalaliDateTime, account: String? = null, card: String? = null,
    ): ParseResult.Transaction {
        val r = parse(s)
        assertIs<ParseResult.Transaction>(r, s.name)
        assertEquals(bank, r.bankId, "${s.name}: bank")
        assertEquals(dir, r.direction, "${s.name}: direction")
        assertEquals(amount, r.amountRial, "${s.name}: amount")
        assertEquals(balance, r.balanceRial, "${s.name}: balance")
        assertEquals(date, r.dateTime, "${s.name}: date")
        if (account != null) assertEquals(account, r.account, "${s.name}: account")
        if (card != null) assertEquals(card, r.counterpartyCard, "${s.name}: card")
        assertTrue(r.confidence >= 0.7, "${s.name}: confidence ${r.confidence}")
        return r
    }

    @Test fun parsian() {
        txn(Fixtures.parsianCredit, "parsian", CREDIT, 19_975_617, 20_553_955, JalaliDateTime(1405, 8, 12, 12, 41), "11110000222233")
        txn(Fixtures.parsianDebit, "parsian", DEBIT, 20_014_500, 539_455, JalaliDateTime(1405, 8, 12, 14, 14), "11110000222233")
        assertEquals(ParseResult.Ignored("parsian", IgnoreReason.LOGIN), parse(Fixtures.parsianLogin))
    }

    @Test fun blu() {
        val r = txn(Fixtures.bluDebit, "blu", DEBIT, 400_000, 17_369_366, JalaliDateTime(1405, 7, 1, 7, 43))
        assertNull(r.account, "Blu sends no account number; the date line must not be read as one")
        txn(Fixtures.bluDebit2, "blu", DEBIT, 800_000, 14_504_638, JalaliDateTime(1405, 8, 10, 14, 9))
        assertEquals(ParseResult.Ignored("blu", IgnoreReason.LOGIN), parse(Fixtures.bluWelcome))
    }

    @Test fun mellat() {
        txn(Fixtures.mellatDebit, "mellat", DEBIT, 5_011_000, 1_009_122, JalaliDateTime(1405, 8, 8, 10, 28), "1234567890")
        txn(Fixtures.mellatCredit, "mellat", CREDIT, 5_000_000, 6_020_122, JalaliDateTime(1405, 8, 8, 8, 34), "1234567890")
        assertEquals(ParseResult.Ignored("mellat", IgnoreReason.LOGIN), parse(Fixtures.mellatLogin))
    }

    @Test fun resalat() {
        val fee = txn(Fixtures.resalatSmsFee, "resalat", DEBIT, 39_000, 19_901_476, JalaliDateTime(1405, 8, 4, 9, 23), "10.99990000.1")
        assertEquals("کارمزد پیامک مرداد ماه 1405", fee.description)
        assertEquals("bank_fee", fee.categoryHint)
        txn(Fixtures.resalatFee, "resalat", DEBIT, 14_500, 19_886_976, JalaliDateTime(1405, 8, 7, 23, 5))
        txn(Fixtures.resalatDebit, "resalat", DEBIT, 15_000_000, 4_886_976, JalaliDateTime(1405, 8, 7, 23, 5))
        txn(Fixtures.resalatCredit, "resalat", CREDIT, 15_000_000, 19_886_976, JalaliDateTime(1405, 8, 8, 10, 29))
        assertEquals(ParseResult.Ignored("resalat", IgnoreReason.LOGIN), parse(Fixtures.resalatLogin))
    }

    @Test fun shahr() {
        txn(Fixtures.shahrDebit, "shahr", DEBIT, 1_811_000, 107_100, JalaliDateTime(1405, 6, 22, 18, 58, 6), "7000000000001")
        assertEquals(ParseResult.Ignored("shahr", IgnoreReason.PROMO), parse(Fixtures.shahrBirthday))
        assertIs<ParseResult.Ignored>(parse(Fixtures.shahrNotice))
    }

    @Test fun otpKeepsOnlyHintsAndNeverTheCode() {
        for (s in listOf(Fixtures.parsianOtp, Fixtures.shahrOtp)) {
            val r = parse(s)
            assertIs<ParseResult.Otp>(r, s.name)
            assertEquals("600000*1234", r.counterpartyCard)
            assertTrue(!r.toString().contains("11223344") && !r.toString().contains("554433"), "OTP code leaked: $r")
        }
        assertEquals(20_000_000, (parse(Fixtures.parsianOtp) as ParseResult.Otp).amountRial)
        assertEquals(1_800_000, (parse(Fixtures.shahrOtp) as ParseResult.Otp).amountRial)
    }

    @Test fun personalMessagesAreNeverRead() {
        assertEquals(ParseResult.NotBank, parse(Fixtures.friend))
        assertEquals(ParseResult.NotBank, parse(Fixtures.personal))
    }

    @Test fun unknownBankStillReadByGenericRules() {
        val r = parser.parse("SomeNewBank", "بانک جدید\nبرداشت از حساب 1234-5678\nمبلغ: 250,000 ریال\nمانده: 3,000,000 ریال\n1405/08/12 09:15", Jalali.toEpoch(JalaliDateTime(1405, 8, 12, 9, 16)))
        assertIs<ParseResult.Transaction>(r)
        assertEquals(DEBIT, r.direction)
        assertEquals(250_000, r.amountRial)
        assertEquals(3_000_000, r.balanceRial)
        assertEquals("generic", r.parser)
    }

    @Test fun unreadableBankMessageGoesToReviewNotTrash() {
        val r = parser.parse("Bank Mellat", "حساب1234567890\nتراکنش ناموفق\nمانده1,009,122", Jalali.toEpoch(JalaliDateTime(1405, 8, 8, 10, 0)))
        assertIs<ParseResult.NeedsReview>(r)
    }
}

/**
 * Layouts for banks we have NO real sample of yet. They are SYNTHETIC, written from formats that are
 * common across Iranian banks, so the generic rules are proven before a real sample arrives.
 * When a real sample arrives, add it to Fixtures and keep these.
 */
class GenericLayoutsTest {
    private val parser = SmsParser()
    private val now = Jalali.toEpoch(JalaliDateTime(1405, 8, 12, 10, 30))
    private fun t(sender: String, body: String) = parser.parse(sender, body, now)

    private fun check(r: ParseResult, dir: Direction, amount: Long, balance: Long?) {
        assertIs<ParseResult.Transaction>(r, r.toString())
        assertEquals(dir, r.direction, r.toString())
        assertEquals(amount, r.amountRial, r.toString())
        assertEquals(balance, r.balanceRial, r.toString())
    }

    @Test fun keywordColonStyle() = check(
        t("Bank Melli", "بانک ملی ایران\nبرداشت:1,250,000\nحساب:0100000000001\nمانده:8,750,000\n1405/08/12-10:20"),
        Direction.DEBIT, 1_250_000, 8_750_000)

    @Test fun trailingSignRtlStyle() = check(
        t("TejaratBank", "بانک تجارت\nحساب 1234-5678\n1,500,000-\nمانده: 3,200,000\n08/12 10:20"),
        Direction.DEBIT, 1_500_000, 3_200_000)

    @Test fun accountingTermsBedehkarBestankar() {
        check(t("BankSaderat", "بانک صادرات\nبستانکار: 7,000,000\nمانده: 9,000,000\n1405/08/12 09:00"), Direction.CREDIT, 7_000_000, 9_000_000)
        check(t("BankSaderat", "بانک صادرات\nبدهکار: 200,000\nمانده: 8,800,000\n1405/08/12 09:10"), Direction.DEBIT, 200_000, 8_800_000)
    }

    @Test fun cardPurchaseWithArabicLetters() = check(
        t("BankSepah", "بانك سپه\nخريد كارتي\nمبلغ: 450,000 ريال\nموجودي: 1,050,000 ريال\n1405/08/12 13:45:10"),
        Direction.DEBIT, 450_000, 1_050_000)

    @Test fun englishLayout() = check(
        t("PasargadBank", "Bank Pasargad\nWithdrawal: 2,000,000\nBalance: 5,000,000\n1405/08/12 11:00"),
        Direction.DEBIT, 2_000_000, 5_000_000)

    @Test fun tomanIsConvertedToRial() = check(
        t("Bank Mellat", "حساب1234567890\nواریز 150,000 تومان\nمانده 2,000,000 تومان\n05/08/12-12:00"),
        Direction.CREDIT, 1_500_000, 20_000_000)

    @Test fun refundIsCreditWithHint() {
        val r = t("Bank Shahr", "بانک شهر\nعودت وجه خرید\nمبلغ:300,000ریال\nموجودی:1,300,000 ریال\n1405/08/12 10:00:00")
        check(r, Direction.CREDIT, 300_000, 1_300_000)
        assertEquals("refund", (r as ParseResult.Transaction).kindHint)
    }

    @Test fun failedTransactionIsMarked() {
        val r = t("Bank Shahr", "بانک شهر\nخرید ناموفق\nمبلغ:300,000ریال\nموجودی:1,300,000 ریال\n1405/08/12 10:00:00")
        assertEquals("failed", (r as ParseResult.Transaction).kindHint)
    }

    @Test fun persianDigitsEverywhere() = check(
        t("Bank Melli", "بانک ملی\nواریز:۲٬۵۰۰٬۰۰۰\nمانده:۱۲٬۵۰۰٬۰۰۰\n۱۴۰۵/۰۸/۱۲ ۰۹:۱۵"),
        Direction.CREDIT, 2_500_000, 12_500_000)

    @Test fun negativeBalance() = check(
        t("ResalatBank", "10.99990000.1\n-500,000\n08/12_10:00\nمانده: -200,000"),
        Direction.DEBIT, 500_000, -200_000)
}
