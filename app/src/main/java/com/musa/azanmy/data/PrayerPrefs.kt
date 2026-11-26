package com.musa.azanmy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "azanmy_prefs")

data class UserSettingsState(
    val isOnboardingDone: Boolean = false,
    val zoneCode: String = "SBH09",
    val selectedDailyAzanId: String = AzanAudioCatalog.DEFAULT_DAILY_ID,
    val themePreference: ThemePreference = ThemePreference.DARK,
    val showStatusNotification: Boolean = true,
    val autoBoostAlarmVolume: Boolean = true,
    val lastSyncMillis: Long = 0L,
    val alertModes: Map<PrayerName, AzanAlertMode> = PrayerName.fardhuList.associateWith { AzanAlertMode.AZAN }
)

class PrayerPrefs(private val context: Context) {

    companion object {
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_ZONE_CODE = stringPreferencesKey("zone_code")
        private val KEY_DAILY_AZAN_ID = stringPreferencesKey("daily_azan_id")
        private val KEY_THEME_PREF = stringPreferencesKey("theme_pref")
        private val KEY_SHOW_STATUS_NOTIF = booleanPreferencesKey("show_status_notif")
        private val KEY_AUTO_BOOST_VOLUME = booleanPreferencesKey("auto_boost_alarm_volume")
        private val KEY_LAST_SYNC_MILLIS = longPreferencesKey("last_sync_millis")
        private val KEY_CACHED_SCHEDULE = stringPreferencesKey("cached_schedule_json")

        private fun keyForPrayerMode(prayer: PrayerName) =
            stringPreferencesKey("mode_${prayer.name.lowercase()}")
    }

    val settingsFlow: Flow<UserSettingsState> = context.dataStore.data.map { prefs ->
        val modes = PrayerName.fardhuList.associateWith { prayer ->
            AzanAlertMode.fromCode(prefs[keyForPrayerMode(prayer)])
        }
        UserSettingsState(
            isOnboardingDone = prefs[KEY_ONBOARDING_DONE] ?: false,
            zoneCode = prefs[KEY_ZONE_CODE] ?: "SBH09",
            selectedDailyAzanId = prefs[KEY_DAILY_AZAN_ID] ?: AzanAudioCatalog.DEFAULT_DAILY_ID,
            themePreference = ThemePreference.fromCode(prefs[KEY_THEME_PREF]),
            showStatusNotification = prefs[KEY_SHOW_STATUS_NOTIF] ?: true,
            autoBoostAlarmVolume = prefs[KEY_AUTO_BOOST_VOLUME] ?: true,
            lastSyncMillis = prefs[KEY_LAST_SYNC_MILLIS] ?: 0L,
            alertModes = modes
        )
    }

    val scheduleFlow: Flow<Map<String, DailyPrayerTimes>> = context.dataStore.data.map { prefs ->
        decodeScheduleJson(prefs[KEY_CACHED_SCHEDULE] ?: "[]")
    }

    suspend fun getSettings(): UserSettingsState = settingsFlow.first()

    suspend fun getScheduleMap(): Map<String, DailyPrayerTimes> = scheduleFlow.first()

    suspend fun getTodayAndTomorrow(): Pair<DailyPrayerTimes?, DailyPrayerTimes?> {
        val map = getScheduleMap()
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        return Pair(map[today.toString()], map[tomorrow.toString()])
    }

    suspend fun completeOnboarding(zoneCode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ZONE_CODE] = zoneCode
            prefs[KEY_ONBOARDING_DONE] = true
        }
    }

    suspend fun setZoneCode(zoneCode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ZONE_CODE] = zoneCode
        }
    }

    suspend fun setSelectedDailyAzanId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DAILY_AZAN_ID] = id
        }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_PREF] = theme.code
        }
    }

    suspend fun setShowStatusNotification(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_STATUS_NOTIF] = enabled
        }
    }

    suspend fun setAutoBoostAlarmVolume(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_BOOST_VOLUME] = enabled
        }
    }

    suspend fun setPrayerAlertMode(prayer: PrayerName, mode: AzanAlertMode) {
        context.dataStore.edit { prefs ->
            prefs[keyForPrayerMode(prayer)] = mode.code
        }
    }

    suspend fun mergeAndSaveSchedule(
        newDays: List<DailyPrayerTimes>,
        replaceAllForZone: Boolean = false
    ): Boolean {
        var hasChanges = false
        val cutoffDate = LocalDate.now().minusDays(5).toString()

        context.dataStore.edit { prefs ->
            val currentMap = if (replaceAllForZone) {
                mutableMapOf()
            } else {
                decodeScheduleJson(prefs[KEY_CACHED_SCHEDULE] ?: "[]").toMutableMap()
            }

            for (day in newDays) {
                val existing = currentMap[day.dateIso]
                if (existing != day) {
                    hasChanges = true
                }
                currentMap[day.dateIso] = day
            }

            val pruned = currentMap.values
                .filter { it.dateIso >= cutoffDate }
                .sortedBy { it.dateIso }

            prefs[KEY_CACHED_SCHEDULE] = encodeScheduleJson(pruned)
            prefs[KEY_LAST_SYNC_MILLIS] = System.currentTimeMillis()
        }

        return hasChanges
    }

    private fun encodeScheduleJson(days: List<DailyPrayerTimes>): String {
        val array = JSONArray()
        for (d in days) {
            val obj = JSONObject()
            obj.put("dateIso", d.dateIso)
            obj.put("hijriRaw", d.hijriRaw)
            obj.put("hijriFormatted", d.hijriFormatted)
            obj.put("dayName", d.dayName)
            obj.put("subuh", d.subuh)
            obj.put("syuruk", d.syuruk)
            obj.put("zohor", d.zohor)
            obj.put("asar", d.asar)
            obj.put("maghrib", d.maghrib)
            obj.put("isyak", d.isyak)
            array.put(obj)
        }
        return array.toString()
    }

    private fun decodeScheduleJson(raw: String): Map<String, DailyPrayerTimes> {
        return try {
            val array = JSONArray(raw)
            val map = LinkedHashMap<String, DailyPrayerTimes>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val dateIso = obj.optString("dateIso", "")
                if (dateIso.isBlank()) continue
                map[dateIso] = DailyPrayerTimes(
                    dateIso = dateIso,
                    hijriRaw = obj.optString("hijriRaw", ""),
                    hijriFormatted = obj.optString("hijriFormatted", ""),
                    dayName = obj.optString("dayName", ""),
                    subuh = obj.optString("subuh", ""),
                    syuruk = obj.optString("syuruk", ""),
                    zohor = obj.optString("zohor", ""),
                    asar = obj.optString("asar", ""),
                    maghrib = obj.optString("maghrib", ""),
                    isyak = obj.optString("isyak", "")
                )
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
