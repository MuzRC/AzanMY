package com.musa.azanmy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.musa.azanmy.MainActivity
import com.musa.azanmy.R
import com.musa.azanmy.alarm.PrayerSyncWorker
import com.musa.azanmy.data.JakimZoneData
import com.musa.azanmy.data.PrayerName
import com.musa.azanmy.data.PrayerPrefs
import com.musa.azanmy.data.PrayerTimeCalculator
import com.musa.azanmy.service.PrayerStatusNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_RESTORE_STATUS_NOTIF = "com.musa.azanmy.action.RESTORE_STATUS_NOTIF"
        const val ACTION_SYNC_FROM_NOTIF = "com.musa.azanmy.action.SYNC_FROM_NOTIF"

        private const val COLOR_GOLD_BRIGHT = 0xFFE6C252.toInt()
        private const val COLOR_WHITE_PRIMARY = 0xFFF5F7F6.toInt()
        private const val COLOR_MUTED_MINT = 0xFFA3C4BC.toInt()

        fun updateAllWidgets(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val component = ComponentName(appContext, PrayerWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return

            CoroutineScope(Dispatchers.IO).launch {
                val views = buildRemoteViews(appContext)
                for (id in ids) {
                    manager.updateAppWidget(id, views)
                }
            }
        }

        suspend fun buildRemoteViews(context: Context): RemoteViews {
            val prefs = PrayerPrefs(context)
            val settings = prefs.getSettings()
            val zone = JakimZoneData.findByCode(settings.zoneCode)
            val (today, tomorrow) = prefs.getTodayAndTomorrow()
            val nextPrayer = PrayerTimeCalculator.findNextPrayer(today, tomorrow)

            val views = RemoteViews(context.packageName, R.layout.widget_prayer_times)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                4001,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            views.setTextViewText(R.id.tv_widget_zone, zone.shortTitle)
            views.setTextViewText(
                R.id.tv_widget_hijri,
                today?.hijriFormatted?.takeIf { it.isNotBlank() } ?: "AzanMY"
            )

            if (nextPrayer != null) {
                views.setTextViewText(
                    R.id.tv_widget_next_label,
                    if (nextPrayer.isTomorrow) "WAKTU ESOK" else "WAKTU SETERUSNYA"
                )
                views.setTextViewText(R.id.tv_widget_next_name, nextPrayer.prayer.displayName)
                views.setTextViewText(R.id.tv_widget_next_time, nextPrayer.time12)
            } else {
                views.setTextViewText(R.id.tv_widget_next_label, "JADUAL SOLAT")
                views.setTextViewText(R.id.tv_widget_next_name, "Tiada Data")
                views.setTextViewText(R.id.tv_widget_next_time, "--:--")
            }

            val columns = listOf(
                Triple(PrayerName.SUBUH, R.id.col_subuh, Pair(R.id.tv_subuh_label, R.id.tv_subuh_time)),
                Triple(PrayerName.ZOHOR, R.id.col_zohor, Pair(R.id.tv_zohor_label, R.id.tv_zohor_time)),
                Triple(PrayerName.ASAR, R.id.col_asar, Pair(R.id.tv_asar_label, R.id.tv_asar_time)),
                Triple(PrayerName.MAGHRIB, R.id.col_maghrib, Pair(R.id.tv_maghrib_label, R.id.tv_maghrib_time)),
                Triple(PrayerName.ISYAK, R.id.col_isyak, Pair(R.id.tv_isyak_label, R.id.tv_isyak_time))
            )

            for ((prayer, colId, textIds) in columns) {
                val (labelId, timeId) = textIds
                val timeText = today?.getTime12For(prayer)?.substringBefore(" ") ?: "--:--"
                val isHighlighted = nextPrayer != null && !nextPrayer.isTomorrow && nextPrayer.prayer == prayer

                views.setTextViewText(timeId, timeText)
                if (isHighlighted) {
                    views.setInt(colId, "setBackgroundResource", R.drawable.bg_widget_active_pill)
                    views.setTextColor(labelId, COLOR_GOLD_BRIGHT)
                    views.setTextColor(timeId, COLOR_GOLD_BRIGHT)
                } else {
                    views.setInt(colId, "setBackgroundResource", 0)
                    views.setTextColor(labelId, COLOR_MUTED_MINT)
                    views.setTextColor(timeId, COLOR_WHITE_PRIMARY)
                }
            }

            return views
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_RESTORE_STATUS_NOTIF -> {
                val appContext = context.applicationContext
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        PrayerStatusNotification.update(appContext)
                    } finally {
                        pendingResult.finish()
                    }
                }
                return
            }

            ACTION_SYNC_FROM_NOTIF -> {
                val appContext = context.applicationContext
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        PrayerSyncWorker.syncSchedules(appContext, forceReplace = true)
                        PrayerStatusNotification.update(appContext)
                    } finally {
                        pendingResult.finish()
                    }
                }
                return
            }
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = buildRemoteViews(appContext)
                for (id in appWidgetIds) {
                    appWidgetManager.updateAppWidget(id, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
