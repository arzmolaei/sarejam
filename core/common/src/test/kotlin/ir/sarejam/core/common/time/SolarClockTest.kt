package ir.sarejam.core.common.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class SolarClockTest {
    private val tehran = ZoneId.of("Asia/Tehran")

    @Test fun tehranSummerAndWinterFollowSunRatherThanFixedSixOClock() {
        fun night(local: String) = SolarClock.isNight(
            java.time.LocalDateTime.parse(local).atZone(tehran).toInstant(), SolarLocation.Tehran, tehran,
        )
        assertFalse(night("2026-06-21T05:30:00"))
        assertFalse(night("2026-06-21T19:00:00"))
        assertTrue(night("2026-06-21T20:00:00"))
        assertTrue(night("2026-12-21T06:30:00"))
        assertFalse(night("2026-12-21T12:00:00"))
        assertTrue(night("2026-12-21T17:30:00"))
    }

    @Test fun southernHemisphereUsesTheLocalDate() {
        val zone = ZoneId.of("Australia/Sydney")
        val location = SolarLocation(-33.8688, 151.2093)
        fun night(local: String) = SolarClock.isNight(
            java.time.LocalDateTime.parse(local).atZone(zone).toInstant(), location, zone,
        )
        assertFalse(night("2026-12-21T06:00:00"))
        assertFalse(night("2026-12-21T19:30:00"))
        assertTrue(night("2026-12-21T22:00:00"))
    }

    @Test fun sunsetAfterMidnightDoesNotTurnIcelandicNoonDark() {
        val location = SolarLocation(64.1466, -21.9426)
        val zone = ZoneId.of("Atlantic/Reykjavik")
        fun night(time: String) = SolarClock.isNight(Instant.parse("2026-06-21T${time}:00Z"), location, zone)
        assertFalse(night("12:00"))
        assertFalse(night("00:00"))
        assertTrue(night("01:00"))
        assertFalse(night("03:15"))
    }

    @Test fun internationalDateLineReferenceCityIsSupported() {
        val location = SolarLocation(1.8721, -157.4278)
        val zone = ZoneId.of("Pacific/Kiritimati")
        val day = java.time.LocalDateTime.parse("2026-06-21T12:00:00").atZone(zone).toInstant()
        val night = java.time.LocalDateTime.parse("2026-06-21T00:00:00").atZone(zone).toInstant()
        assertFalse(SolarClock.isNight(day, location, zone))
        assertTrue(SolarClock.isNight(night, location, zone))
    }

    @Test fun polarDayAndNightHaveDefinedBehavior() {
        val north = SolarLocation(89.0, 0.0)
        val zone = ZoneId.of("UTC")
        assertFalse(SolarClock.isNight(Instant.parse("2026-06-21T00:00:00Z"), north, zone))
        assertTrue(SolarClock.isNight(Instant.parse("2026-12-21T12:00:00Z"), north, zone))
    }

    @Test fun invalidCoordinatesAreRejected() {
        assertFailsWith<IllegalArgumentException> { SolarLocation(91.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { SolarLocation(0.0, Double.NaN) }
        assertFailsWith<IllegalArgumentException> { SolarLocation(0.0, 181.0) }
    }
}
