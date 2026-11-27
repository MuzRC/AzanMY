package com.musa.azanmy.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import java.util.concurrent.TimeUnit

object JakimApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jakimDateInputFormat = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("d-MMM-yyyy")
        .toFormatter(Locale.ENGLISH)

    private val jakimNumericDateFormat = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("d-MM-yyyy")
        .toFormatter(Locale.ENGLISH)

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
        val cleanZone = zoneCode.trim().uppercase(Locale.ENGLISH)
        val monthPrefix = yearMonth.toString()
        val startDate = yearMonth.atDay(1).toString()
        val endDate = yearMonth.atEndOfMonth().toString()

        // 1. Kaedah utama: POST period=duration (diperlukan e-Solat JAKIM untuk julat tarikh/bulan depan)
        val durationUrl =
            "https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=duration&zone=$cleanZone"
        val primaryResult = executeFetchWithRetry(
            fullUrl = durationUrl,
            zoneCode = cleanZone,
            postFields = mapOf(
                "datestart" to startDate,
                "dateend" to endDate
            ),
            filterMonthPrefix = monthPrefix
        )
        if (primaryResult.isSuccess && !primaryResult.getOrNull().isNullOrEmpty()) {
            return@withContext primaryResult
        }

        // 2. Fallback bulan semasa: GET period=month
        if (yearMonth == YearMonth.now()) {
            val monthUrl =
                "https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=month&zone=$cleanZone"
            val monthResult = executeFetchWithRetry(
                fullUrl = monthUrl,
                zoneCode = cleanZone,
                filterMonthPrefix = monthPrefix
            )
            if (monthResult.isSuccess && !monthResult.getOrNull().isNullOrEmpty()) {
                return@withContext monthResult
            }
        }

        // 3. Fallback tahun semasa (termasuk bulan depan dalam tahun sama): GET period=year dan tapis bulan sasaran
        if (yearMonth.year == LocalDate.now().year) {
            val yearUrl =
                "https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=year&zone=$cleanZone"
            val yearResult = executeFetchWithRetry(
                fullUrl = yearUrl,
                zoneCode = cleanZone,
                filterMonthPrefix = monthPrefix
            )
            if (yearResult.isSuccess && !yearResult.getOrNull().isNullOrEmpty()) {
                return@withContext yearResult
            }
        }

        primaryResult
    }

    private suspend fun executeFetchWithRetry(
        fullUrl: String,
        zoneCode: String,
        postFields: Map<String, String>? = null,
        filterMonthPrefix: String? = null
    ): Result<List<DailyPrayerTimes>> {
        var lastError: Exception? = null

        repeat(2) { attempt ->
            try {
                val requestBuilder = Request.Builder()
                    .url(fullUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 16) AzanMY/1.0")
                    .header("Accept", "application/json")

                if (postFields != null) {
                    val formBuilder = FormBody.Builder()
                    postFields.forEach { (k, v) -> formBuilder.add(k, v) }
                    requestBuilder.post(formBuilder.build())
                } else {
                    requestBuilder.get()
                }

                client.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Pelayan JAKIM memulangkan kod HTTP ${response.code}")
                    }
                    val body = response.body?.string()
                        ?: throw IllegalStateException("Respons kosong daripada pelayan JAKIM")
                    val parsedAll = parseJakimJson(body)
                    val parsed = if (filterMonthPrefix != null) {
                        parsedAll.filter { it.dateIso.startsWith(filterMonthPrefix) }
                    } else {
                        parsedAll
                    }
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
        val trimmed = rawDate.trim()
        if (trimmed.isBlank()) return ""
        if (trimmed.matches(Regex("\d{4}-\d{2}-\d{2}"))) {
            return trimmed
        }
        return try {
            LocalDate.parse(trimmed, jakimDateInputFormat).toString()
        } catch (_: Exception) {
            try {
                LocalDate.parse(trimmed, jakimNumericDateFormat).toString()
            } catch (_: Exception) {
                ""
            }
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
