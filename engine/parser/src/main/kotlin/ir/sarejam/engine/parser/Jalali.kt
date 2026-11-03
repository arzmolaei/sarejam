package ir.sarejam.engine.parser

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Jalali ⇄ Gregorian conversion (arithmetic algorithm, valid for 1300–1500 AP). */
object Jalali {
    val TEHRAN: ZoneId = ZoneId.of("Asia/Tehran")

    private val gDaysBeforeMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)

    fun fromGregorian(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 + gd + gDaysBeforeMonth[gm - 1]
        var jy = -1595 + 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm = if (days < 186) 1 + days / 31 else 7 + (days - 186) / 30
        val jd = 1 + if (days < 186) days % 31 else (days - 186) % 30
        return Triple(jy, jm, jd)
    }

    fun toGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val jy1 = jy + 1595
        var days = -355668 + 365 * jy1 + (jy1 / 33) * 8 + ((jy1 % 33) + 3) / 4 + jd +
            if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            days -= 1
            gy += 100 * (days / 36524)
            days %= 36524
            if (days >= 365) days += 1
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val leap = (gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0
        val monthDays = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 12 && gd > monthDays[gm]) {
            gd -= monthDays[gm]
            gm++
        }
        return Triple(gy, gm + 1, gd)
    }

    fun fromEpoch(epochMillis: Long, zone: ZoneId = TEHRAN): JalaliDateTime {
        val t = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zone)
        val (y, m, d) = fromGregorian(t.year, t.monthValue, t.dayOfMonth)
        return JalaliDateTime(y, m, d, t.hour, t.minute, t.second)
    }

    fun toEpoch(j: JalaliDateTime, zone: ZoneId = TEHRAN): Long {
        val (y, m, d) = toGregorian(j.year, j.month, j.day)
        return LocalDateTime.of(y, m, d, j.hour, j.minute, j.second).atZone(zone).toInstant().toEpochMilli()
    }
}
