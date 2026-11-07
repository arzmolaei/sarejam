package ir.sarejam.core.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DisplayRulesTest {
    @Test
    fun automaticTimeChangesAtSixAndEighteen() {
        assertTrue(isAutomaticDarkHour(0))
        assertTrue(isAutomaticDarkHour(5))
        assertFalse(isAutomaticDarkHour(6))
        assertFalse(isAutomaticDarkHour(17))
        assertTrue(isAutomaticDarkHour(18))
        assertTrue(isAutomaticDarkHour(23))
    }

    @Test
    fun signedAmountsPreserveEveryDigitIncludingLongExtremes() {
        assertEquals("+9,223,372,036,854,775,807", formatSignedAmount(Long.MAX_VALUE, false))
        assertEquals("−9,223,372,036,854,775,808", formatSignedAmount(Long.MIN_VALUE, false))
        assertEquals("+۱۲٬۳۴۵", formatSignedAmount(12_345, true))
        assertEquals("−۱۲٬۳۴۵", formatSignedAmount(-12_345, true))
        assertEquals("۰", formatSignedAmount(0, true))
    }
}
