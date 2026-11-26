package com.musa.azanmy.alarm

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.musa.azanmy.data.JakimApiService
import com.musa.azanmy.data.PrayerPrefs
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.TimeUnit

class PrayerSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val syncResult = syncSchedules(applicationContext, forceReplace = false)
        return if (syncResult.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "azanmy_weekly_jakim_sync"
        private const val SEVEN_DAYS_MILLIS = 7L * 24L * 60L * 60L * 1000L

        fun enqueueWeeklySync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<PrayerSyncWorker>(7, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        suspend fun syncIfStale(context: Context) {
            val prefs = PrayerPrefs(context.applicationContext)
            val settings = prefs.getSettings()
            val (today, tomorrow) = prefs.getTodayAndTomorrow()
            val isExpired = (System.currentTimeMillis() - settings.lastSyncMillis) >= SEVEN_DAYS_MILLIS

            if (today == null || tomorrow == null || isExpired) {
                syncSchedules(context.applicationContext, forceReplace = false)
            }
        }

        suspend fun syncSchedules(
            context: Context,
            forceReplace: Boolean = false
        ): kotlin.Result<Boolean> {
            val appContext = context.applicationContext
            val prefs = PrayerPrefs(appContext)
            val settings = prefs.getSettings()
            val zoneCode = settings.zoneCode

            val currentMonth = YearMonth.now()
            val currentResult = JakimApiService.fetchMonthSchedule(zoneCode, currentMonth)
            val currentDays = currentResult.getOrElse { err ->
                return kotlin.Result.failure(err)
            }

            var hasChanges = prefs.mergeAndSaveSchedule(
                newDays = currentDays,
                replaceAllForZone = forceReplace
            )

            val today = LocalDate.now()
            val cachedMap = prefs.getScheduleMap()
            val tomorrowMissing = cachedMap[today.plusDays(1).toString()] == null

            if (today.dayOfMonth >= 25 || tomorrowMissing) {
                val nextMonth = currentMonth.plusMonths(1)
                val nextResult = JakimApiService.fetchMonthSchedule(zoneCode, nextMonth)
                nextResult.getOrNull()?.let { nextDays ->
                    if (nextDays.isNotEmpty()) {
                        val nextChanged = prefs.mergeAndSaveSchedule(
                            newDays = nextDays,
                            replaceAllForZone = false
                        )
                        hasChanges = hasChanges || nextChanged
                    }
                }
            }

            AzanScheduler.scheduleNext(appContext)
            return kotlin.Result.success(hasChanges)
        }
    }
}
