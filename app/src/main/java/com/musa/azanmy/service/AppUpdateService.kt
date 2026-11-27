package com.musa.azanmy.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.musa.azanmy.MainActivity
import com.musa.azanmy.R
import com.musa.azanmy.data.AppReleaseInfo
import com.musa.azanmy.util.AppUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class AppUpdateService : Service() {

    companion object {
        private const val CHANNEL_ID_PROGRESS = "azan_app_update_progress_channel"
        private const val CHANNEL_ID_RESULT = "azan_app_update_result_channel"

        private const val NOTIF_ID_PROGRESS = 2050
        private const val NOTIF_ID_RESULT = 2051

        const val ACTION_START_DOWNLOAD = "com.musa.azanmy.action.START_UPDATE_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.musa.azanmy.action.CANCEL_UPDATE_DOWNLOAD"

        private const val EXTRA_VERSION_CODE = "extra_version_code"
        private const val EXTRA_VERSION_NAME = "extra_version_name"
        private const val EXTRA_APK_URL = "extra_apk_url"
        private const val EXTRA_RELEASE_NOTES = "extra_release_notes"
        private const val EXTRA_UPDATED_AT = "extra_updated_at"

        fun startDownload(context: Context, release: AppReleaseInfo) {
            val appCtx = context.applicationContext
            val intent = Intent(appCtx, AppUpdateService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_VERSION_CODE, release.versionCode)
                putExtra(EXTRA_VERSION_NAME, release.versionName)
                putExtra(EXTRA_APK_URL, release.apkUrl)
                putExtra(EXTRA_RELEASE_NOTES, release.releaseNotes)
                putExtra(EXTRA_UPDATED_AT, release.updatedAt)
            }
            ContextCompat.startForegroundService(appCtx, intent)
        }

        fun cancelDownload(context: Context) {
            val appCtx = context.applicationContext
            AppUpdater.abortActiveNetworkCall()
            val intent = Intent(appCtx, AppUpdateService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
            }
            try {
                appCtx.startService(intent)
            } catch (_: Exception) {
                AppUpdater.markCancelled()
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureNotificationChannels()

        if (intent?.action == ACTION_CANCEL_DOWNLOAD) {
            AppUpdater.abortActiveNetworkCall()
            downloadJob?.cancel()
            downloadJob = null
            AppUpdater.markCancelled()
            releaseLocks()
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }

        val versionCode = intent?.getIntExtra(EXTRA_VERSION_CODE, 0) ?: 0
        val versionName = intent?.getStringExtra(EXTRA_VERSION_NAME).orEmpty().ifBlank { "Baharu" }
        val apkUrl = intent?.getStringExtra(EXTRA_APK_URL).orEmpty()
        val releaseNotes = intent?.getStringExtra(EXTRA_RELEASE_NOTES).orEmpty()
        val updatedAt = intent?.getStringExtra(EXTRA_UPDATED_AT).orEmpty()

        val initialNotif = buildProgressNotification(
            versionName = versionName,
            progress = 0,
            indeterminate = true,
            detailText = "Menyambung ke pelayan GitHub..."
        )
        startForegroundCompat(initialNotif)

        if (apkUrl.isBlank()) {
            AppUpdater.markError("Pautan muat turun APK tidak sah.")
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }

        if (downloadJob?.isActive == true) {
            return START_NOT_STICKY
        }

        val releaseInfo = AppReleaseInfo(
            versionCode = versionCode,
            versionName = versionName,
            apkUrl = apkUrl,
            releaseNotes = releaseNotes,
            updatedAt = updatedAt
        )

        acquireLocks()
        cancelResultNotification()

        downloadJob = serviceScope.launch {
            var lastNotifTime = 0L
            var lastNotifPercent = -1
            var lastNotifStatus = ""

            try {
                val apkFile = AppUpdater.downloadApkWithResume(
                    context = applicationContext,
                    releaseInfo = releaseInfo
                ) { percent, downloadedBytes, totalBytes, statusMsg ->
                    val now = SystemClock.elapsedRealtime()
                    val shouldUpdate = (percent != lastNotifPercent && (now - lastNotifTime >= 450L || percent == 100)) ||
                        (statusMsg != lastNotifStatus) ||
                        (now - lastNotifTime >= 1_200L)

                    if (shouldUpdate) {
                        lastNotifTime = now
                        lastNotifPercent = percent
                        lastNotifStatus = statusMsg

                        val sizeText = if (totalBytes > 0L) {
                            "${AppUpdater.formatBytes(downloadedBytes)} / ${AppUpdater.formatBytes(totalBytes)} ($percent%)"
                        } else if (downloadedBytes > 0L) {
                            "${AppUpdater.formatBytes(downloadedBytes)} dimuat turun"
                        } else {
                            statusMsg
                        }

                        val detail = if (statusMsg.startsWith("Menyambung")) statusMsg else sizeText
                        val notif = buildProgressNotification(
                            versionName = releaseInfo.versionName,
                            progress = percent,
                            indeterminate = totalBytes <= 0L,
                            detailText = detail
                        )
                        getNotificationManager()?.notify(NOTIF_ID_PROGRESS, notif)
                    }
                }

                AppUpdater.markReadyToInstall(releaseInfo, apkFile.absolutePath)
                showReadyToInstallNotification(releaseInfo, apkFile)
            } catch (_: CancellationException) {
                AppUpdater.markCancelled()
            } catch (e: Exception) {
                val msg = e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Muat turun kemas kini gagal. Sila cuba semula."
                AppUpdater.markError(msg, releaseInfo)
                showDownloadFailedNotification(releaseInfo.versionName, msg)
            } finally {
                downloadJob = null
                releaseLocks()
                stopForegroundCompat()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int) {
        handleSystemTimeout()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        handleSystemTimeout()
    }

    private fun handleSystemTimeout() {
        AppUpdater.abortActiveNetworkCall()
        downloadJob?.cancel()
        downloadJob = null
        AppUpdater.markError("Muat turun dihentikan sementara oleh had masa sistem. Sila tekan semula untuk sambung.")
        releaseLocks()
        stopForegroundCompat()
        stopSelf()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID_PROGRESS,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIF_ID_PROGRESS, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun getNotificationManager(): NotificationManager? =
        getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private fun ensureNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getNotificationManager() ?: return

            val progressChannel = NotificationChannel(
                CHANNEL_ID_PROGRESS,
                "Kemajuan Muat Turun Kemas Kini",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menunjukkan kemajuan muat turun fail kemas kini AzanMY di latar belakang."
                setShowBadge(false)
            }

            val resultChannel = NotificationChannel(
                CHANNEL_ID_RESULT,
                "Status Kemas Kini Aplikasi",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Makluman apabila fail kemas kini AzanMY siap dipasang."
            }

            nm.createNotificationChannel(progressChannel)
            nm.createNotificationChannel(resultChannel)
        }
    }

    private fun buildProgressNotification(
        versionName: String,
        progress: Int,
        indeterminate: Boolean,
        detailText: String
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            2052,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, AppUpdateService::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
        }
        val cancelPending = PendingIntent.getService(
            this,
            2053,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_PROGRESS)
            .setSmallIcon(R.drawable.ic_notif_mosque)
            .setContentTitle("Memuat turun AzanMY v$versionName")
            .setContentText(detailText)
            .setProgress(100, progress.coerceIn(0, 100), indeterminate)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openPending)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Batal",
                cancelPending
            )
            .build()
    }

    private fun showReadyToInstallNotification(release: AppReleaseInfo, apkFile: File) {
        val installIntent = AppUpdater.createInstallIntent(applicationContext, apkFile)
        val installPending = PendingIntent.getActivity(
            this,
            2054,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, CHANNEL_ID_RESULT)
            .setSmallIcon(R.drawable.ic_notif_mosque)
            .setContentTitle("AzanMY v${release.versionName} Sedia Dipasang")
            .setContentText("Muat turun selesai. Ketik untuk memasang kemas kini sekarang.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(installPending)
            .addAction(
                android.R.drawable.stat_sys_download_done,
                "Pasang Sekarang",
                installPending
            )
            .build()

        getNotificationManager()?.notify(NOTIF_ID_RESULT, notif)
    }

    private fun showDownloadFailedNotification(versionName: String, errorMsg: String) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            2055,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, CHANNEL_ID_RESULT)
            .setSmallIcon(R.drawable.ic_notif_mosque)
            .setContentTitle("Muat Turun AzanMY v$versionName Gagal")
            .setContentText(errorMsg)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .build()

        getNotificationManager()?.notify(NOTIF_ID_RESULT, notif)
    }

    private fun cancelResultNotification() {
        getNotificationManager()?.cancel(NOTIF_ID_RESULT)
    }

    private fun acquireLocks() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (wakeLock == null) {
                wakeLock = pm?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "AzanMY::AppUpdateWakeLock"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            wakeLock?.acquire(15 * 60 * 1000L)
        } catch (_: Exception) {
        }

        try {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiLock == null) {
                val wifiMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    @Suppress("DEPRECATION")
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock = wm?.createWifiLock(wifiMode, "AzanMY::AppUpdateWifiLock")?.apply {
                    setReferenceCounted(false)
                }
            }
            if (wifiLock?.isHeld == false) {
                wifiLock?.acquire()
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        } finally {
            wakeLock = null
        }

        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (_: Exception) {
        } finally {
            wifiLock = null
        }
    }

    override fun onDestroy() {
        AppUpdater.abortActiveNetworkCall()
        downloadJob?.cancel()
        downloadJob = null
        releaseLocks()
        serviceScope.cancel()
        super.onDestroy()
    }
}
