package com.musa.azanmy.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SystemEventReceiver : BroadcastReceiver() {

    private val allowedActions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
    )

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in allowedActions) return

        val appContext = context.applicationContext
        val userManager = appContext.getSystemService(Context.USER_SERVICE) as? UserManager
        if (userManager?.isUserUnlocked == false) {
            return
        }

        val pendingResult = goAsync()
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = try {
            pm?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AzanMY::SystemEventWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(20_000L)
            }
        } catch (_: Exception) {
            null
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                PrayerSyncWorker.enqueueWeeklySync(appContext)
                AzanScheduler.scheduleNext(appContext)
            } catch (_: Exception) {
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (_: Exception) {
                }
                pendingResult.finish()
            }
        }
    }
}
