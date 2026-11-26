package com.musa.azanmy.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.musa.azanmy.data.AzanAlertMode
import com.musa.azanmy.data.PrayerName
import com.musa.azanmy.data.PrayerPrefs
import com.musa.azanmy.service.AzanAudioPlayer
import com.musa.azanmy.service.AzanForegroundService
import com.musa.azanmy.service.PrayerStatusNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AzanReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != AzanScheduler.ACTION_PRAYER_ALARM &&
            action != AzanScheduler.ACTION_MIDNIGHT_REFRESH
        ) {
            return
        }

        val appContext = context.applicationContext
        val pendingResult = goAsync()

        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val tempWakeLock = pm?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AzanMY::ReceiverWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(15_000L)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (action == AzanScheduler.ACTION_PRAYER_ALARM) {
                    val prayer = PrayerName.fromName(
                        intent.getStringExtra(AzanScheduler.EXTRA_PRAYER_NAME)
                    )
                    if (prayer != null && prayer.isFardhu) {
                        val settings = PrayerPrefs(appContext).getSettings()
                        val mode = settings.alertModes[prayer] ?: AzanAlertMode.AZAN

                        when (mode) {
                            AzanAlertMode.AZAN -> {
                                AzanForegroundService.startAzan(appContext, prayer)
                            }
                            AzanAlertMode.VIBRATE -> {
                                AzanAudioPlayer.vibrateThreeTimes(appContext)
                                PrayerStatusNotification.showPrayerAlertBanner(appContext, prayer)
                            }
                            AzanAlertMode.SILENT -> {
                                // Kemas kini notifikasi & widget secara senyap sahaja
                            }
                        }
                    }
                }

                AzanScheduler.scheduleNext(appContext)
            } finally {
                try {
                    if (tempWakeLock?.isHeld == true) {
                        tempWakeLock.release()
                    }
                } catch (_: Exception) {
                }
                pendingResult.finish()
            }
        }
    }
}
