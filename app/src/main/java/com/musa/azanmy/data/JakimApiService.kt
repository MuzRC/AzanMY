package com.musa.azanmy.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

object JakimApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jakimDateInputFormat = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH)

    private val hijriMonths = listOf(
        "Muharam",
        "Safar",
        "Rabiulawal",
        "Rabiulakhir",
        "Jamadilawal",
        "Jamadilakhir",
        "Rejab",
        "Syaaban",
        "Ramadan",
        "Syawal",
        "Zulkaedah",
        "Zulhijjah"
    )

    suspend fun fetchMonthSchedule(
        zoneCode: String,
        yearMonth: YearMonth = YearMonth.now()
    ): Result<List<DailyPrayerTimes>> = withContext(Dispatchers.IO) {
        val startDate = yearMonth.atDay(1).toString()
        val endDate = yearMonth.atEndOfMonth().toString()
        val durationUrl =
            "https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=duration&datestart=$startDate&dateend=$endDate"

        val primaryResult = executeFetchWithRetry(durationUrl, zoneCode)
        if (primaryResult.isSuccess && !primaryResult.getOrNull().isNullOrEmpty()) {
            return@withContext primaryResult
        }

        if (yearMonth == YearMonth.now()) {
            val monthUrl =
                "https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=month"
            val fallbackResult = executeFetchWithRetry(monthUrl, zoneCode)
            if (fallbackResult.isSuccess && !fallbackResult.getOrNull().isNullOrEmpty()) {
                return@withContext fallbackResult
            }
        }

        primaryResult
    }

    private suspend fun executeFetchWithRetry(
        baseUrl: String,
        zoneCode: String
    ): Result<List<DailyPrayerTimes>> {
        var lastError: Exception? = null
        val fullUrl = "$baseUrl&zone=${zoneCode.uppercase(Locale.ENGLISH)}"

        repeat(2) { attempt ->
            try {
                val request = Request.Builder()
                    .url(fullUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 16) AzanMY/1.0")
                    .header("Accept", "application/json")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Pelayan JAKIM memulangkan kod HTTP ${response.code}")
                    }
                    val body = response.body?.string()
                        ?: throw IllegalStateException("Respons kosong daripada pelayan JAKIM")
                    val parsed = parseJakimJson(body)
                    if (parsed.isEmpty()) {
                        throw IllegalStateException("Tiada rekod waktu solat dijumpai untuk zon $zoneCode")
                    }
                    return Result.success(parsed)
                }
            } catch (e: Exception) {
                lastError = e
                if (attempt == 0) delay(1_200L)
            }
        }

        return Result.failure(lastError ?: IllegalStateException("Gagal berhubung dengan portal e-Solat JAKIM"))
    }

    private fun parseJakimJson(rawJson: String): List<DailyPrayerTimes> {
        val root = JSONObject(rawJson)
        val array = root.optJSONArray("prayerTime") ?: return emptyList()
        val list = ArrayList<DailyPrayerTimes>(array.length())

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val rawDate = item.optString("date", "")
            val dateIso = normalizeDateToIso(rawDate)
            if (dateIso.isBlank()) continue

            val hijriRaw = item.optString("hijri", "")
            val dayEng = item.optString("day", "")

            list.add(
                DailyPrayerTimes(
                    dateIso = dateIso,
                    hijriRaw = hijriRaw,
                    hijriFormatted = formatHijriDate(hijriRaw),
                    dayName = translateDayToMalay(dayEng, dateIso),
                    subuh = cleanTime(item.optString("fajr", "")),
                    syuruk = cleanTime(item.optString("syuruk", "")),
                    zohor = cleanTime(item.optString("dhuhr", "")),
                    asar = cleanTime(item.optString("asr", "")),
                    maghrib = cleanTime(item.optString("maghrib", "")),
                    isyak = cleanTime(item.optString("isha", ""))
                )
            )
        }
        return list
    }

    private fun normalizeDateToIso(rawDate: String): String {
        return try {
            if (rawDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                rawDate
            } else {
                LocalDate.parse(rawDate.trim(), jakimDateInputFormat).toString()
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun cleanTime(rawTime: String): String {
        val parts = rawTime.trim().split(":")
        return if (parts.size >= 2) {
            "${parts[0].padStart(2, '0')}:${parts[1].padStart(2, '0')}"
        } else {
            rawTime.take(5)
        }
    }

    fun formatHijriDate(hijriRaw: String): String {
        return try {
            val parts = hijriRaw.trim().split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val monthIdx = parts[1].toInt() - 1
                val day = parts[2].toInt()
                val monthName = hijriMonths.getOrNull(monthIdx) ?: return hijriRaw
                "$day $monthName ${year}H"
            } else {
                hijriRaw
            }
        } catch (_: Exception) {
            hijriRaw
        }
    }

    private fun translateDayToMalay(dayEng: String, dateIso: String): String {
        return when (dayEng.trim().lowercase(Locale.ENGLISH)) {
            "monday" -> "Isnin"
            "tuesday" -> "Selasa"
            "wednesday" -> "Rabu"
            "thursday" -> "Khamis"
            "friday" -> "Jumaat"
            "saturday" -> "Sabtu"
            "sunday" -> "Ahad"
            else -> try {
                when (LocalDate.parse(dateIso).dayOfWeek.value) {
                    1 -> "Isnin"
                    2 -> "Selasa"
                    3 -> "Rabu"
                    4 -> "Khamis"
                    5 -> "Jumaat"
                    6 -> "Sabtu"
                    else -> "Ahad"
                }
            } catch (_: Exception) {
                dayEng
            }
        }
    }
}
