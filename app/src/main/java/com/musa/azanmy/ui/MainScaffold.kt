package com.musa.azanmy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musa.azanmy.R
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (ext.isDark) {
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0A231E), Color(0xFF04100D))
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFFFFF), Color(0xFFF2F6F4))
                            )
                        }
                    )
            ) {
                // Garis Halus Emas di Atas Bar Navigasi
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    ext.goldAccent.copy(alpha = 0.65f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tab 1: Waktu Solat dengan Ikon Masjid & Menara Berkembar
                    LuxuryNavItem(
                        label = "Waktu Solat",
                        selected = selectedTab == 0,
                        onClick = {
                            AzanAudioPlayer.stopAll(invokeCallback = false)
                            selectedTab = 0
                        },
                        modifier = Modifier.weight(1f)
                    ) { isSelected ->
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Waktu Solat",
                            modifier = Modifier
                                .size(34.dp)
                                .alpha(if (isSelected) 1f else 0.55f)
                        )
                    }

                    // Tab 2: Tetapan dengan Ikon Bintang Geometri Islamik (Rub el Hizb Gear)
                    LuxuryNavItem(
                        label = "Tetapan",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    ) { isSelected ->
                        IslamicGearIcon(
                            tint = if (isSelected) ext.goldBright else ext.subtleText,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
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

@Composable
private fun LuxuryNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (Boolean) -> Unit
) {
    val ext = LocalExtendedColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.height(34.dp),
            contentAlignment = Alignment.Center
        ) {
            icon(selected)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) ext.goldBright else ext.subtleText
        )
    }
}

@Composable
private fun IslamicGearIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.068f
        val halfSide = size.minDimension * 0.31f
        val center = Offset(size.width / 2f, size.height / 2f)

        val squarePath = Path().apply {
            moveTo(center.x - halfSide, center.y - halfSide)
            lineTo(center.x + halfSide, center.y - halfSide)
            lineTo(center.x + halfSide, center.y + halfSide)
            lineTo(center.x - halfSide, center.y + halfSide)
            close()
        }

        // Bintang 8 Bucu Rub el Hizb
        drawPath(
            path = squarePath,
            color = tint,
            style = Stroke(width = strokeWidth)
        )
        rotate(degrees = 45f, pivot = center) {
            drawPath(
                path = squarePath,
                color = tint,
                style = Stroke(width = strokeWidth)
            )
        }

        // Bulatan Gear di Tengah
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.145f,
            center = center,
            style = Stroke(width = strokeWidth)
        )
    }
}
