package com.musa.azanmy

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.musa.azanmy.alarm.AzanScheduler
import com.musa.azanmy.alarm.PrayerSyncWorker
import com.musa.azanmy.service.AzanForegroundService
import com.musa.azanmy.service.PrayerStatusNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AzanApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        PrayerSyncWorker.enqueueWeeklySync(this)
        appScope.launch {
            AzanScheduler.scheduleNext(applicationContext)
        }
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val alarmChannel = NotificationChannel(
            AzanForegroundService.CHANNEL_ID_ALARM,
            "Laungan Azan Masuk Waktu",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifikasi aktif semasa azan sedang berkumandang."
            setSound(null, null)
            enableVibration(false)
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val statusChannel = NotificationChannel(
            PrayerStatusNotification.CHANNEL_ID_STATUS,
            "Jadual Solat di Bar Status",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Paparan senyap waktu solat seterusnya pada bar notifikasi."
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val alertChannel = NotificationChannel(
            PrayerStatusNotification.CHANNEL_ID_ALERT,
            "Peringatan Masuk Waktu",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Pemberitahuan masuk waktu apabila mod getar dipilih."
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        nm.createNotificationChannels(listOf(alarmChannel, statusChannel, alertChannel))
    }
}
