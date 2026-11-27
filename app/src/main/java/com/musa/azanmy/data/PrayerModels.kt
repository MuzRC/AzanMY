package com.musa.azanmy.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class PrayerName(
    val displayName: String,
    val requestCode: Int,
    val isFardhu: Boolean
) {
    SUBUH("Subuh", 1001, true),
    SYURUK("Syuruk", 1002, false),
    ZOHOR("Zohor", 1003, true),
    ASAR("Asar", 1004, true),
    MAGHRIB("Maghrib", 1005, true),
    ISYAK("Isyak", 1006, true);

    companion object {
        val fardhuList: List<PrayerName> = listOf(SUBUH, ZOHOR, ASAR, MAGHRIB, ISYAK)

        fun fromName(name: String?): PrayerName? =
            entries.find { it.name.equals(name, ignoreCase = true) }
    }
}

enum class AzanAlertMode(val code: String, val label: String) {
    AZAN("AZAN", "Azan Penuh"),
    VIBRATE("VIBRATE", "Getar Sahaja"),
    SILENT("SILENT", "Senyap");

    fun next(): AzanAlertMode = when (this) {
        AZAN -> VIBRATE
        VIBRATE -> SILENT
        SILENT -> AZAN
    }

    companion object {
        fun fromCode(code: String?): AzanAlertMode =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: AZAN
    }
}

enum class ThemePreference(val code: String, val label: String) {
    SYSTEM("SYSTEM", "Ikut Sistem"),
    DARK("DARK", "Mod Gelap"),
    LIGHT("LIGHT", "Mod Cerah");

    companion object {
        fun fromCode(code: String?): ThemePreference =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: DARK
    }
}

data class JakimZone(
    val code: String,
    val state: String,
    val areas: String,
    val lat: Double,
    val lon: Double
) {
    val primaryArea: String
        get() = areas.substringBefore(",").trim()

    val shortTitle: String
        get() = "$code • $primaryArea"
}

data class DailyPrayerTimes(
    val dateIso: String,
    val hijriRaw: String,
    val hijriFormatted: String,
    val dayName: String,
    val subuh: String,
    val syuruk: String,
    val zohor: String,
    val asar: String,
    val maghrib: String,
    val isyak: String
) {
    fun getTime24For(prayer: PrayerName): String = when (prayer) {
        PrayerName.SUBUH -> subuh
        PrayerName.SYURUK -> syuruk
        PrayerName.ZOHOR -> zohor
        PrayerName.ASAR -> asar
        PrayerName.MAGHRIB -> maghrib
        PrayerName.ISYAK -> isyak
    }

    fun getTime12For(prayer: PrayerName): String =
        PrayerTimeCalculator.format24To12(getTime24For(prayer))
}

data class NextPrayerInfo(
    val prayer: PrayerName,
    val triggerMillis: Long,
    val time24: String,
    val time12: String,
    val isTomorrow: Boolean
)

data class AppReleaseInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String,
    val updatedAt: String
)

object PrayerTimeCalculator {
    private val formatter12h = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
    private val formatter24h = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

    private fun parseLocalTimeSafe(time24: String): LocalTime? {
        val parts = time24.trim().split(":")
        if (parts.size < 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return LocalTime.of(hour, minute, 0, 0)
    }

    private fun format24Short(time24: String): String {
        val time = parseLocalTimeSafe(time24) ?: return time24.trim().take(5)
        return time.format(formatter24h)
    }

    fun format24To12(time24: String): String {
        return try {
            val time = parseLocalTimeSafe(time24) ?: return time24.trim().take(5)
            time.format(formatter12h)
        } catch (_: Exception) {
            time24.trim().take(5)
        }
    }

    fun parseTimeToMillis(
        dateIso: String,
        time24: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        return try {
            val date = LocalDate.parse(dateIso.trim())
            val time = parseLocalTimeSafe(time24) ?: return 0L
            ZonedDateTime.of(date, time, zoneId).toInstant().toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }

    fun getNextMidnightMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val tomorrow = LocalDate.now(zoneId).plusDays(1)
        return tomorrow.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun findNextPrayer(
        today: DailyPrayerTimes?,
        tomorrow: DailyPrayerTimes?,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): NextPrayerInfo? {
        if (today != null) {
            for (prayer in PrayerName.fardhuList) {
                val time24 = today.getTime24For(prayer)
                val millis = parseTimeToMillis(today.dateIso, time24, zoneId)
                if (millis > nowMillis) {
                    return NextPrayerInfo(
                        prayer = prayer,
                        triggerMillis = millis,
                        time24 = format24Short(time24),
                        time12 = format24To12(time24),
                        isTomorrow = false
                    )
                }
            }
        }

        if (tomorrow != null) {
            val subuh24 = tomorrow.subuh
            val millis = parseTimeToMillis(tomorrow.dateIso, subuh24, zoneId)
            if (millis > nowMillis) {
                return NextPrayerInfo(
                    prayer = PrayerName.SUBUH,
                    triggerMillis = millis,
                    time24 = format24Short(subuh24),
                    time12 = format24To12(subuh24),
                    isTomorrow = true
                )
            }
        }

        if (today != null) {
            val fallbackDate = try {
                LocalDate.parse(today.dateIso.trim()).plusDays(1).toString()
            } catch (_: Exception) {
                LocalDate.now(zoneId).plusDays(1).toString()
            }
            val subuh24 = today.subuh
            val millis = parseTimeToMillis(fallbackDate, subuh24, zoneId)
            if (millis > nowMillis) {
                return NextPrayerInfo(
                    prayer = PrayerName.SUBUH,
                    triggerMillis = millis,
                    time24 = format24Short(subuh24),
                    time12 = format24To12(subuh24),
                    isTomorrow = true
                )
            }
        }

        return null
    }

    fun getCurrentActivePrayer(
        today: DailyPrayerTimes?,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): PrayerName? {
        if (today == null) return null
        var current: PrayerName? = null
        for (prayer in PrayerName.fardhuList) {
            val millis = parseTimeToMillis(today.dateIso, today.getTime24For(prayer), zoneId)
            if (millis in 1..nowMillis) {
                current = prayer
            }
        }
        return current
    }
}
