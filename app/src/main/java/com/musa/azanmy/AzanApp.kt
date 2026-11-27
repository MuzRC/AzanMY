package com.musa.azanmy

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.UserManager
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

        val userManager = getSystemService(Context.USER_SERVICE) as? UserManager
        val isUnlocked = userManager?.isUserUnlocked ?: true
        if (!isUnlocked) {
            return
        }

        try {
            PrayerSyncWorker.enqueueWeeklySync(this)
        } catch (_: Exception) {
        }

        appScope.launch {
            try {
                AzanScheduler.scheduleNext(applicationContext)
            } catch (_: Exception) {
            }
        }
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // Padam saluran lama (IMPORTANCE_LOW) supaya Android tidak simpan tetapan lama di bahagian Silent
        try {
            nm.deleteNotificationChannel(PrayerStatusNotification.LEGACY_CHANNEL_ID_STATUS)
        } catch (_: Exception) {
        }

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

        // IMPORTANCE_DEFAULT tanpa bunyi/getar supaya duduk di bahagian atas notifikasi utama, bukan di Silent
        val statusChannel = NotificationChannel(
            PrayerStatusNotification.CHANNEL_ID_STATUS,
            "Jadual Solat di Panel Utama",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Paparan jadual waktu solat pada bahagian utama panel notifikasi tanpa bunyi."
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
