package com.musa.azanmy.data

import android.annotation.SuppressLint
import android.content.Context

data class AzanAudioOption(
    val id: String,
    val rawName: String,
    val title: String,
    val muadzin: String
)

object AzanAudioCatalog {

    const val DEFAULT_DAILY_ID = "jakim"

    val subuhFixedTrack = AzanAudioOption(
        id = "subuh",
        rawName = "subuh",
        title = "Azan Subuh (As-Salatu Khairum Minan Naum)",
        muadzin = "Laungan Khas Waktu Subuh"
    )

    val dailyOptions: List<AzanAudioOption> = listOf(
        AzanAudioOption(
            id = "jakim",
            rawName = "jakim",
            title = "Azan Rasmi Malaysia (JAKIM)",
            muadzin = "Ustaz Radzi Kamarul Hailan"
        ),
        AzanAudioOption(
            id = "makkah",
            rawName = "makkah",
            title = "Azan Masjidil Haram (Makkah)",
            muadzin = "Syeikh Ali Ahmed Mulla"
        ),
        AzanAudioOption(
            id = "madinah",
            rawName = "madinah",
            title = "Azan Masjid An-Nabawi (Madinah)",
            muadzin = "Syeikh Essam Bukhari"
        ),
        AzanAudioOption(
            id = "al_aqsa",
            rawName = "al_aqsa",
            title = "Azan Masjid Al-Aqsa (Palestin)",
            muadzin = "Syeikh Naji Qazzaz"
        ),
        AzanAudioOption(
            id = "mishary",
            rawName = "mishary",
            title = "Azan Alunan Syeikh Mishary",
            muadzin = "Syeikh Mishary Rashid Al-Afasy"
        ),
        AzanAudioOption(
            id = "takbir",
            rawName = "takbir",
            title = "Takbir Hari Raya (Aidilfitri & Adha)",
            muadzin = "Kumpulan Qari Malaysia"
        )
    )

    fun findDailyOption(id: String): AzanAudioOption {
        return dailyOptions.find { it.id == id } ?: dailyOptions.first()
    }

    fun getRawNameForPrayer(prayer: PrayerName, selectedDailyId: String): String {
        return if (prayer == PrayerName.SUBUH) {
            subuhFixedTrack.rawName
        } else {
            findDailyOption(selectedDailyId).rawName
        }
    }

    @SuppressLint("DiscouragedApi")
    fun resolveRawResId(context: Context, rawName: String): Int {
        return try {
            context.resources.getIdentifier(rawName, "raw", context.packageName)
        } catch (_: Exception) {
            0
        }
    }

    fun isAudioUploaded(context: Context, rawName: String): Boolean {
        return resolveRawResId(context, rawName) != 0
    }
}
