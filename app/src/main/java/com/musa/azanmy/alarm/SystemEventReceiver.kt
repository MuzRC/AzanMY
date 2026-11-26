package com.musa.azanmy.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SystemEventReceiver : BroadcastReceiver() {

    private val allowedActions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_LOCKED_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_SET,
        Intent.ACTION_TIMEZONE_CHANGED,
        AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
    )

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in allowedActions) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                PrayerSyncWorker.enqueueWeeklySync(appContext)
                AzanScheduler.scheduleNext(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
