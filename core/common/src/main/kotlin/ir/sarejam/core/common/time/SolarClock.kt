package ir.sarejam.core.common.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.*

/** A manually selected position; the app never requests location permission. */
data class SolarLocation(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }

    companion object { val Tehran = SolarLocation(35.6892, 51.3890) }
}

/** Sunrise/sunset approximation from the US Naval Observatory almanac algorithm.
 * Uses the standard 90.833° zenith (refraction and the sun's apparent radius).
 * Accuracy is suitable for display themes, not civil or religious timekeeping.
 */
object SolarClock {
    fun isNight(instant: Instant, location: SolarLocation, zone: ZoneId): Boolean {
        val date = instant.atZone(zone).toLocalDate()
        val rise = event(date, location, zone, sunrise = true)
        if (rise is Event.Polar) return !rise.daylight
        val set = event(date, location, zone, sunrise = false)
        if (set is Event.Polar) return !set.daylight
        val sunrise = (rise as Event.Time).instant
        val sunset = (set as Event.Time).instant
        return if (sunrise <= sunset) {
            instant < sunrise || instant >= sunset
        } else {
            // At high latitudes the sunset can belong to the early hours of the
            // calendar day. Its short nighttime interval ends at that day's sunrise.
            instant >= sunset && instant < sunrise
        }
    }

    private sealed interface Event {
        data class Time(val instant: Instant) : Event
        data class Polar(val daylight: Boolean) : Event
    }

    private fun event(date: LocalDate, location: SolarLocation, zone: ZoneId, sunrise: Boolean): Event {
        val longitudeHour = location.longitude / 15.0
        val t = date.dayOfYear + ((if (sunrise) 6.0 else 18.0) - longitudeHour) / 24.0
        val meanAnomaly = 0.9856 * t - 3.289
        val longitude = wrap(meanAnomaly + 1.916 * sin(rad(meanAnomaly)) +
            0.020 * sin(2 * rad(meanAnomaly)) + 282.634, 360.0)
        var rightAscension = wrap(deg(atan(0.91764 * tan(rad(longitude)))), 360.0)
        rightAscension += floor(longitude / 90.0) * 90.0 - floor(rightAscension / 90.0) * 90.0
        rightAscension /= 15.0
        val sinDeclination = 0.39782 * sin(rad(longitude))
        val cosDeclination = cos(asin(sinDeclination))
        val cosHour = (cos(rad(90.833)) - sinDeclination * sin(rad(location.latitude))) /
            (cosDeclination * cos(rad(location.latitude)))
        if (cosHour > 1.0) return Event.Polar(daylight = false)
        if (cosHour < -1.0) return Event.Polar(daylight = true)
        val hourAngle = (if (sunrise) 360.0 - deg(acos(cosHour)) else deg(acos(cosHour))) / 15.0
        val utcHour = wrap(hourAngle + rightAscension - 0.06571 * t - 6.622 - longitudeHour, 24.0)
        var result = date.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds((utcHour * 3600).roundToLong())
        // Normalizing UTC hours can cross the local calendar date, e.g. a Sydney sunrise.
        val correction = ChronoUnit.DAYS.between(result.atZone(zone).toLocalDate(), date)
        result = result.plusSeconds(correction * 86_400L)
        return Event.Time(result)
    }

    private fun rad(degrees: Double) = Math.toRadians(degrees)
    private fun deg(radians: Double) = Math.toDegrees(radians)
    private fun wrap(value: Double, modulus: Double) = ((value % modulus) + modulus) % modulus
}
