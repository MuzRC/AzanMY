package com.musa.azanmy.alarm

import android.content.Context
import androidx.work.BackoffPolicy
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
        if (syncResult.isFailure) {
            return if (runAttemptCount < 4) Result.retry() else Result.failure()
        }

        // Pastikan jadual bulan hadapan wujud apabila menghampiri hujung bulan
        val today = LocalDate.now()
        if (today.dayOfMonth >= NEXT_MONTH_PREFETCH_DAY) {
            val firstOfNextMonth = YearMonth.now().plusMonths(1).atDay(1).toString()
            val cachedMap = PrayerPrefs(applicationContext).getScheduleMap()
            if (cachedMap[firstOfNextMonth] == null && runAttemptCount < 4) {
                return Result.retry()
            }
        }

        return Result.success()
    }

    companion object {
        private const val LEGACY_UNIQUE_WORK_NAME = "azanmy_weekly_jakim_sync"
        private const val UNIQUE_WORK_NAME = "azanmy_periodic_jakim_sync_v2"
        private const val SYNC_INTERVAL_DAYS = 2L
        private const val NEXT_MONTH_PREFETCH_DAY = 20
        private const val SEVEN_DAYS_MILLIS = 7L * 24L * 60L * 60L * 1000L

        fun enqueueWeeklySync(context: Context) {
            val wm = WorkManager.getInstance(context.applicationContext)
            try {
                wm.cancelUniqueWork(LEGACY_UNIQUE_WORK_NAME)
            } catch (_: Exception) {
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<PrayerSyncWorker>(SYNC_INTERVAL_DAYS, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            wm.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        suspend fun syncIfStale(context: Context) {
            val prefs = PrayerPrefs(context.applicationContext)
            val settings = prefs.getSettings()
            val (today, tomorrow) = prefs.getTodayAndTomorrow()
            val nowDate = LocalDate.now()
            val cachedMap = prefs.getScheduleMap()
            val nextMonthFirst = YearMonth.now().plusMonths(1).atDay(1).toString()
            val needsNextMonth = nowDate.dayOfMonth >= NEXT_MONTH_PREFETCH_DAY && cachedMap[nextMonthFirst] == null
            val isExpired = (System.currentTimeMillis() - settings.lastSyncMillis) >= SEVEN_DAYS_MILLIS

            if (today == null || tomorrow == null || needsNextMonth || isExpired) {
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
            val upcomingMissing = (1L..5L).any { cachedMap[today.plusDays(it).toString()] == null }

            var nextMonthFailure: Throwable? = null
            if (today.dayOfMonth >= NEXT_MONTH_PREFETCH_DAY || tomorrowMissing || upcomingMissing) {
                val nextMonth = currentMonth.plusMonths(1)
                val nextResult = JakimApiService.fetchMonthSchedule(zoneCode, nextMonth)
                val nextDays = nextResult.getOrNull()
                if (!nextDays.isNullOrEmpty()) {
                    val nextChanged = prefs.mergeAndSaveSchedule(
                        newDays = nextDays,
                        replaceAllForZone = false
                    )
                    hasChanges = hasChanges || nextChanged
                } else {
                    nextMonthFailure = nextResult.exceptionOrNull()
                }
            }

            AzanScheduler.scheduleNext(appContext)

            if (tomorrowMissing && nextMonthFailure != null) {
                return kotlin.Result.failure(nextMonthFailure)
            }

            return kotlin.Result.success(hasChanges)
        }
    }
}
