package com.musa.azanmy.service

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.musa.azanmy.MainActivity
import com.musa.azanmy.R
import com.musa.azanmy.data.JakimZoneData
import com.musa.azanmy.data.PrayerName
import com.musa.azanmy.data.PrayerPrefs
import com.musa.azanmy.data.PrayerTimeCalculator
import com.musa.azanmy.widget.PrayerWidgetProvider
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object PrayerStatusNotification {

    const val LEGACY_CHANNEL_ID_STATUS = "azan_status_channel"
    const val CHANNEL_ID_STATUS = "azan_status_channel_v2"
    const val CHANNEL_ID_ALERT = "azan_banner_channel"

    private const val NOTIF_ID_STATUS = 1001
    private const val NOTIF_ID_BANNER_BASE = 1100

    private val compact12hFormatter = DateTimeFormatter.ofPattern("h:mm", Locale.ENGLISH)

    fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private fun formatCompact12(time24: String): String {
        return try {
            LocalTime.parse(time24.trim().take(5)).format(compact12hFormatter)
        } catch (_: Exception) {
            time24.take(5)
        }
    }

    suspend fun update(context: Context) {
        val appContext = context.applicationContext
        val prefs = PrayerPrefs(appContext)
        val settings = prefs.getSettings()

        if (!settings.showStatusNotification || !canPostNotifications(appContext)) {
            cancel(appContext)
            return
        }

        val zone = JakimZoneData.findByCode(settings.zoneCode)
        val (today, tomorrow) = prefs.getTodayAndTomorrow()
        val nextPrayer = PrayerTimeCalculator.findNextPrayer(today, tomorrow)

        val hijriPart = today?.hijriFormatted?.takeIf { it.isNotBlank() }
        val headerTitle = if (hijriPart != null) {
            "${zone.code} (${zone.primaryArea}) • $hijriPart"
        } else {
            "${zone.code} (${zone.primaryArea})"
        }

        val subHeader = if (today != null && hijriPart != null) {
            "${today.dayName}, $hijriPart"
        } else {
            "Waktu Solat Rasmi JAKIM"
        }

        val nextLine = if (nextPrayer != null) {
            val esokTag = if (nextPrayer.isTomorrow) " (Esok)" else ""
            "⏳ Seterusnya: ${nextPrayer.prayer.displayName} (${nextPrayer.time12})$esokTag"
        } else {
            "Sila tekan Segerak untuk muat turun jadual JAKIM"
        }

        val expandedBody = if (today != null) {
            val row1 = "Subuh ${formatCompact12(today.subuh)} • Syuruk ${formatCompact12(today.syuruk)} • Zohor ${formatCompact12(today.zohor)}"
            val row2 = "Asar ${formatCompact12(today.asar)} • Maghrib ${formatCompact12(today.maghrib)} • Isyak ${formatCompact12(today.isyak)}"
            "$nextLine\n\n$row1\n$row2"
        } else {
            nextLine
        }

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            appContext,
            1010,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val syncIntent = Intent(appContext, PrayerWidgetProvider::class.java).apply {
            action = PrayerWidgetProvider.ACTION_SYNC_FROM_NOTIF
        }
        val syncPendingIntent = PendingIntent.getBroadcast(
            appContext,
            1012,
            syncIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val restoreIntent = Intent(appContext, PrayerWidgetProvider::class.java).apply {
            action = PrayerWidgetProvider.ACTION_RESTORE_STATUS_NOTIF
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            appContext,
            1011,
            restoreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID_STATUS)
            .setSmallIcon(R.drawable.ic_notif_mosque)
            .setContentTitle(headerTitle)
            .setContentText(nextLine)
            .setSubText(subHeader)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(headerTitle)
                    .bigText(expandedBody)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSortKey("0000")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(openPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .addAction(0, "Buka", openPendingIntent)
            .addAction(0, "Segerak", syncPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(appContext).notify(NOTIF_ID_STATUS, notification)
        } catch (_: SecurityException) {
        }
    }

    fun showPrayerAlertBanner(context: Context, prayer: PrayerName) {
        val appContext = context.applicationContext
        if (!canPostNotifications(appContext)) return

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            appContext,
            NOTIF_ID_BANNER_BASE + prayer.requestCode,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID_ALERT)
            .setSmallIcon(R.drawable.ic_notif_mosque)
            .setContentTitle("Telah Masuk Waktu ${prayer.displayName}")
            .setContentText("Waktu solat fardhu ${prayer.displayName} telah tiba.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(appContext)
                .notify(NOTIF_ID_BANNER_BASE + prayer.ordinal, notification)
        } catch (_: SecurityException) {
        }
    }

    fun cancel(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(NOTIF_ID_STATUS)
        } catch (_: Exception) {
        }
    }
}
