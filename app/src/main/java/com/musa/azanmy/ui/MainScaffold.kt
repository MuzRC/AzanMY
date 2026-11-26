package com.musa.azanmy.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AccessTimeFilled
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musa.azanmy.alarm.PrayerSyncWorker
import com.musa.azanmy.data.JakimZone
import com.musa.azanmy.data.PrayerPrefs
import com.musa.azanmy.data.UserSettingsState
import com.musa.azanmy.service.AzanAudioPlayer
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun AzanMYRoot() {
    val context = LocalContext.current
    val prefs = remember { PrayerPrefs(context.applicationContext) }
    val settings by prefs.settingsFlow.collectAsState(initial = null)
    val scheduleMap by prefs.scheduleFlow.collectAsState(initial = emptyMap())

    DisposableEffect(Unit) {
        onDispose {
            AzanAudioPlayer.stopAll(invokeCallback = false)
        }
    }

    val currentSettings = settings ?: return

    AzanMYTheme(themePreference = currentSettings.themePreference) {
        if (!currentSettings.isOnboardingDone) {
            SetupScreen(
                initialZoneCode = currentSettings.zoneCode,
                onSetupCompleted = { }
            )
        } else {
            MainScaffold(
                settings = currentSettings,
                scheduleMap = scheduleMap,
                prefs = prefs
            )
        }
    }
}

@Composable
fun MainScaffold(
    settings: UserSettingsState,
    scheduleMap: Map<String, com.musa.azanmy.data.DailyPrayerTimes>,
    prefs: PrayerPrefs
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ext = LocalExtendedColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showZonePicker by remember { mutableStateOf(false) }

    val todayIso = LocalDate.now().toString()
    val tomorrowIso = LocalDate.now().plusDays(1).toString()
    val todayTimes = scheduleMap[todayIso]
    val tomorrowTimes = scheduleMap[tomorrowIso]

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        AzanAudioPlayer.stopAll(invokeCallback = false)
                        selectedTab = 0
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) {
                                Icons.Rounded.AccessTimeFilled
                            } else {
                                Icons.Outlined.AccessTime
                            },
                            contentDescription = "Waktu Solat"
                        )
                    },
                    label = {
                        Text(
                            text = "Waktu Solat",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ext.goldBright,
                        selectedTextColor = ext.goldBright,
                        indicatorColor = ext.goldContainer,
                        unselectedIconColor = ext.subtleText,
                        unselectedTextColor = ext.subtleText
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) {
                                Icons.Rounded.Settings
                            } else {
                                Icons.Outlined.Settings
                            },
                            contentDescription = "Tetapan"
                        )
                    },
                    label = {
                        Text(
                            text = "Tetapan",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ext.goldBright,
                        selectedTextColor = ext.goldBright,
                        indicatorColor = ext.goldContainer,
                        unselectedIconColor = ext.subtleText,
                        unselectedTextColor = ext.subtleText
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    settings = settings,
                    todayTimes = todayTimes,
                    tomorrowTimes = tomorrowTimes,
                    onOpenZonePicker = { showZonePicker = true },
                    onTogglePrayerMode = { prayer, nextMode ->
                        scope.launch {
                            prefs.setPrayerAlertMode(prayer, nextMode)
                            snackbarHostState.showSnackbar(
                                "${prayer.displayName}: ${nextMode.label}"
                            )
                        }
                    },
                    onManualSyncRequested = {
                        scope.launch {
                            val res = PrayerSyncWorker.syncSchedules(context, forceReplace = true)
                            res.onSuccess {
                                snackbarHostState.showSnackbar("Jadual waktu solat JAKIM berjaya dikemas kini.")
                            }.onFailure { err ->
                                snackbarHostState.showSnackbar(
                                    err.message ?: "Gagal memuat turun jadual JAKIM."
                                )
                            }
                        }
                    }
                )

                1 -> SettingsScreen(
                    settings = settings,
                    prefs = prefs,
                    onOpenZonePicker = { showZonePicker = true },
                    onShowMessage = { msg ->
                        scope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                )
            }
        }
    }

    if (showZonePicker) {
        ZonePickerSheet(
            selectedZoneCode = settings.zoneCode,
            onZoneSelected = { zone: JakimZone ->
                showZonePicker = false
                scope.launch {
                    prefs.setZoneCode(zone.code)
                    snackbarHostState.showSnackbar("Memuat turun jadual ${zone.shortTitle}...")
                    val res = PrayerSyncWorker.syncSchedules(context, forceReplace = true)
                    res.onSuccess {
                        snackbarHostState.showSnackbar("Zon ditukar ke ${zone.shortTitle}")
                    }.onFailure { err ->
                        snackbarHostState.showSnackbar(
                            err.message ?: "Zon disimpan, sambung internet untuk muat turun jadual."
                        )
                    }
                }
            },
            onDismiss = { showZonePicker = false }
        )
    }
}
