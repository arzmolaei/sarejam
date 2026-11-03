package ir.sarejam.engine.parser

/**
 * Real message layouts from users' phones, ANONYMISED: names, account numbers, card numbers,
 * OTP codes and IPs are replaced with fake values. Amounts, balances, dates and wording are kept
 * because they are what the parser must read. Never commit a real message without anonymising it.
 */
object Fixtures {
    data class Sms(val name: String, val sender: String, val body: String, val received: JalaliDateTime)

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int) = JalaliDateTime(y, m, d, h, min)

    // ---------------------------------------------------------------- Parsian
    val parsianCredit = Sms("parsian credit", "PARSIANBANK", "11110000222233\nمبلغ:+19,975,617\nمانده:20,553,955\n08/12\n12:41", at(1405, 8, 12, 12, 42))
    val parsianDebit = Sms("parsian debit", "PARSIANBANK", "11110000222233\nمبلغ:-20,014,500\nمانده:539,455\n08/12\n14:14", at(1405, 8, 12, 14, 15))
    val parsianOtp = Sms("parsian otp", "PARSIANBANK", "انتقال به\n600000*1234\nمبلغ 20,000,000\nرمز: 11223344", at(1405, 8, 12, 14, 14))
    val parsianLogin = Sms("parsian login", "PARSIANBANK", "ورود به همراه بانک 1405/08/12\nزمان 14:13:57\nآی پی 10.0.0.1\nدستگاه samsung", at(1405, 8, 12, 14, 14))

    // ---------------------------------------------------------------- Blu
    val bluDebit = Sms("blu debit", "+9890000258", "بلو\nبرداشت پول\nکاربر عزیز، 400,000 ریال از\nحساب شما پرید.\nموجودی: 17,369,366 ریال\n۷:۴۳\n۱۴۰۵.۰۷.۰۱", at(1405, 7, 1, 7, 43))
    val bluDebit2 = Sms("blu debit no space", "+9890000258", "بلو\nبرداشت پول\nکاربر عزیز، 800,000ریال از\nحساب شخصی شما پرید.\nموجودی: 14,504,638 ریال\n۱۴:۰۹\n۱۴۰۵.۰۸.۱۰", at(1405, 8, 10, 14, 9))
    val bluWelcome = Sms("blu welcome", "+9890000258", "بلو\nکاربر عزیز خوش آمدید.\n14:09:07\n1405.08.10", at(1405, 8, 10, 14, 9))

    // ---------------------------------------------------------------- Mellat
    val mellatDebit = Sms("mellat debit", "Bank Mellat", "حساب1234567890\nبرداشت5,011,000\nمانده1,009,122\n05/08/08-10:28", at(1405, 8, 8, 10, 28))
    val mellatCredit = Sms("mellat credit", "Bank Mellat", "حساب1234567890\nواریز5,000,000\nمانده6,020,122\n05/08/08-08:34", at(1405, 8, 8, 8, 34))
    val mellatLogin = Sms("mellat login", "Bank Mellat", "بانک ملت\nبه سامانه همراه بانک ملت خوش\nآمدید.\n1405/08/08 - 00:45", at(1405, 8, 8, 0, 45))

    // ---------------------------------------------------------------- Resalat
    val resalatSmsFee = Sms("resalat sms fee", "ResalatBank", "10.99990000.1\n-39,000\n08/04_09:23\nمانده: 19,901,476\nکارمزد پیامک مرداد ماه 1405", at(1405, 8, 4, 9, 24))
    val resalatFee = Sms("resalat transfer fee", "ResalatBank", "10.99990000.1\n-14,500\n08/07_23:05\nمانده: 19,886,976", at(1405, 8, 7, 23, 5))
    val resalatDebit = Sms("resalat debit", "ResalatBank", "10.99990000.1\n-15,000,000\n08/07_23:05\nمانده: 4,886,976", at(1405, 8, 7, 23, 5))
    val resalatCredit = Sms("resalat credit", "ResalatBank", "10.99990000.1\n+15,000,000\n08/08_10:29\nمانده: 19,886,976", at(1405, 8, 8, 10, 29))
    val resalatLogin = Sms("resalat login", "ResalatBank", "بانک قرض الحسنه رسالت\nورود به همراه بانک\n1405/08/07 23:04", at(1405, 8, 7, 23, 4))

    // ---------------------------------------------------------------- Shahr (note Arabic ي and ك)
    val shahrOtp = Sms("shahr otp", "Bank Shahr", "*بانک شهر*\nانتقال به 600000*1234\nمبلغ: 1,800,000 ریال\nرمز: 554433\nزمان اعتبار رمز: 18:59:55\nتاریخ: 1405/06/22 18:57:55", at(1405, 6, 22, 18, 58))
    val shahrDebit = Sms("shahr debit", "Bank Shahr", "*بانک شهر*\nانتقال وجه كارتي\nبرداشت از:7000000000001\nمبلغ:1,811,000ريال\nموجودي:107,100 ريال\n1405/06/22 18:58:06", at(1405, 6, 22, 18, 58))
    val shahrBirthday = Sms("shahr birthday", "Bank Shahr", "* بانک شهر *\nجناب آقای کاربر دل شهر به بودنتان گرم است، میلادتان فرخنده باد.", at(1405, 6, 30, 9, 36))
    val shahrNotice = Sms("shahr notice", "Bank Shahr", "بانک شهر\nمشتری گرامی به منظور بروزرسانی زیرساخت ها، سامانه‌های بانک شهر از ساعت 1 تا 5 بامداد در دسترس نخواهند بود.", at(1405, 7, 16, 20, 0))

    // ---------------------------------------------------------------- not banks
    val friend = Sms("friend money talk", "+989121234567", "سلام 400,000 ریال واریز کردم به حسابت", at(1405, 8, 1, 10, 0))
    val personal = Sms("personal", "+989121234567", "فردا ساعت ۵ میام", at(1405, 8, 1, 10, 0))
}
