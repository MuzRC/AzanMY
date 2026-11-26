package com.musa.azanmy.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.musa.azanmy.data.PrayerName
import com.musa.azanmy.data.PrayerPrefs
import com.musa.azanmy.data.PrayerTimeCalculator
import com.musa.azanmy.service.PrayerStatusNotification
import com.musa.azanmy.widget.PrayerWidgetProvider

object AzanScheduler {

    const val ACTION_PRAYER_ALARM = "com.musa.azanmy.action.PRAYER_ALARM"
    const val ACTION_MIDNIGHT_REFRESH = "com.musa.azanmy.action.MIDNIGHT_REFRESH"
    const val EXTRA_PRAYER_NAME = "extra_prayer_name"

    private const val REQ_CODE_PRAYER = 3001
    private const val REQ_CODE_MIDNIGHT = 3002

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    suspend fun scheduleNext(context: Context) {
        val appContext = context.applicationContext
        val prefs = PrayerPrefs(appContext)
        val settings = prefs.getSettings()
        val (today, tomorrow) = prefs.getTodayAndTomorrow()

        val nextPrayer = PrayerTimeCalculator.findNextPrayer(today, tomorrow)
        if (nextPrayer != null) {
            scheduleExactPrayerAlarm(
                context = appContext,
                prayer = nextPrayer.prayer,
                triggerMillis = nextPrayer.triggerMillis
            )
        } else {
            cancelPrayerAlarm(appContext)
        }

        val midnightMillis = PrayerTimeCalculator.getNextMidnightMillis() + 1_000L
        scheduleMidnightRefresh(appContext, midnightMillis)

        if (settings.showStatusNotification) {
            PrayerStatusNotification.update(appContext)
        } else {
            PrayerStatusNotification.cancel(appContext)
        }

        PrayerWidgetProvider.updateAllWidgets(appContext)
    }

    private fun scheduleExactPrayerAlarm(
        context: Context,
        prayer: PrayerName,
        triggerMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AzanReceiver::class.java).apply {
            action = ACTION_PRAYER_ALARM
            putExtra(EXTRA_PRAYER_NAME, prayer.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_CODE_PRAYER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    private fun scheduleMidnightRefresh(context: Context, triggerMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AzanReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_REFRESH
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_CODE_MIDNIGHT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    fun cancelPrayerAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AzanReceiver::class.java).apply {
            action = ACTION_PRAYER_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_CODE_PRAYER,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
