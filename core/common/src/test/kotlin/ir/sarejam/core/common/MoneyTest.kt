package ir.sarejam.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test fun groupsWithPersianDigits() = assertEquals("۱٬۲۵۰٬۰۰۰", Money.group(1_250_000))
    @Test fun groupsWithLatinDigits() = assertEquals("1٬250٬000", Money.group(1_250_000, persianDigits = false))
    @Test fun smallAndZero() { assertEquals("۰", Money.group(0)); assertEquals("۹۹۹", Money.group(999)) }
    @Test fun negativeKeepsSign() = assertEquals("−۲۰۰٬۰۰۰", Money.group(-200_000))
    @Test fun extremesDoNotOverflow() = assertEquals("−9٬223٬372٬036٬854٬775٬808", Money.group(Long.MIN_VALUE, false))
    @Test fun tomanIsRialDividedByTen() = assertEquals(150_000L, Money.rialToToman(1_500_000))
}
