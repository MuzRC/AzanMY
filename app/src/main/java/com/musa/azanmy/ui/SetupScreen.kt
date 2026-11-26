package com.musa.azanmy.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.musa.azanmy.R
import com.musa.azanmy.alarm.PrayerSyncWorker
import com.musa.azanmy.data.JakimZone
import com.musa.azanmy.data.JakimZoneData
import com.musa.azanmy.data.PrayerPrefs
import com.musa.azanmy.data.ZoneLocator
import com.musa.azanmy.util.PermissionHelper
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    initialZoneCode: String,
    onSetupCompleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ext = LocalExtendedColors.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedZone by remember(initialZoneCode) {
        mutableStateOf(JakimZoneData.findByCode(initialZoneCode))
    }
    var permStatus by remember { mutableStateOf(PermissionHelper.checkAll(context)) }
    var isDetectingGps by remember { mutableStateOf(false) }
    var gpsMessage by remember { mutableStateOf<String?>(null) }
    var isFinishing by remember { mutableStateOf(false) }
    var showZoneSheet by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permStatus = PermissionHelper.checkAll(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permStatus = PermissionHelper.checkAll(context)
        val granted = results.values.any { it }
        if (granted) {
            scope.launch {
                isDetectingGps = true
                gpsMessage = null
                val res = ZoneLocator.detectNearestZone(context)
                res.onSuccess { zone ->
                    selectedZone = zone
                    gpsMessage = "Zon dikesan: ${zone.shortTitle}"
                }.onFailure { err ->
                    gpsMessage = err.message ?: "Gagal kesan GPS. Sila pilih zon manual."
                }
                isDetectingGps = false
            }
        } else {
            gpsMessage = "Izin lokasi ditolak. Sila pilih zon JAKIM secara manual."
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        permStatus = PermissionHelper.checkAll(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B2922), MidnightEmerald)
                )
            )
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF12362E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Logo AzanMY",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Persediaan Awal AzanMY",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = OffWhiteText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Lengkapkan 4 tetapan ringkas ini supaya azan berkumandang tepat pada waktunya tanpa disekat sistem.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedMintText
                    )
                }
            }

            // Langkah 1: Zon JAKIM & GPS Sekali Imbas
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CardEmerald,
                    border = BorderStroke(1.dp, SubtleMintBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = BrightGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "1. Zon Waktu Solat JAKIM",
                                    fontWeight = FontWeight.SemiBold,
                                    color = OffWhiteText,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${selectedZone.code} • ${selectedZone.areas}",
                                    color = BrightGold,
                                    fontSize = 12.sp,
                                    maxLines = 2
                                )
                            }
                        }

                        if (gpsMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = gpsMessage!!,
                                color = MutedMintText,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    if (permStatus.hasLocation) {
                                        scope.launch {
                                            isDetectingGps = true
                                            gpsMessage = null
                                            ZoneLocator.detectNearestZone(context)
                                                .onSuccess { zone ->
                                                    selectedZone = zone
                                                    gpsMessage = "Zon dikesan: ${zone.shortTitle}"
                                                }
                                                .onFailure { err ->
                                                    gpsMessage = err.message
                                                }
                                            isDetectingGps = false
                                        }
                                    } else {
                                        locationLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                                Manifest.permission.ACCESS_FINE_LOCATION
                                            )
                                        )
                                    }
                                },
                                enabled = !isDetectingGps,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SoftGoldContainer,
                                    contentColor = BrightGold
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isDetectingGps) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = BrightGold
                                    )
                                } else {
                                    Text("Auto GPS", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            OutlinedButton(
                                onClick = { showZoneSheet = true },
                                border = BorderStroke(1.dp, SubtleMintBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Pilih Manual", color = OffWhiteText, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Langkah 2: Notifikasi
            item {
                SetupPermissionCard(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "2. Kebenaran Notifikasi",
                    description = "Diperlukan untuk paparan bar status solat dan kawalan berhenti azan.",
                    isGranted = permStatus.hasNotification,
                    actionLabel = "Benarkan",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            PermissionHelper.openAppDetailsSettings(context)
                        }
                    }
                )
            }

            // Langkah 3: Penggera Tepat
            item {
                SetupPermissionCard(
                    icon = Icons.Rounded.AlarmOn,
                    title = "3. Penggera Tepat (Alarms & Reminders)",
                    description = "Menjamin laungan azan berkumandang tepat pada saat masuk waktu tanpa lewat.",
                    isGranted = permStatus.hasExactAlarm,
                    actionLabel = "Buka Tetapan",
                    onAction = { PermissionHelper.openExactAlarmSettings(context) }
                )
            }

            // Langkah 4: Abaikan Sekatan Bateri
            item {
                SetupPermissionCard(
                    icon = Icons.Rounded.BatteryChargingFull,
                    title = "4. Abaikan Sekatan Bateri",
                    description = "Mengelakkan sistem Android mematikan penggera subuh semasa skrin tidur.",
                    isGranted = permStatus.isBatteryUnrestricted,
                    actionLabel = "Nyahhad Bateri",
                    onAction = { PermissionHelper.requestIgnoreBatteryOptimizations(context) }
                )
            }

            // Butang Teruskan
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        scope.launch {
                            isFinishing = true
                            val prefs = PrayerPrefs(context)
                            prefs.completeOnboarding(selectedZone.code)
                            PrayerSyncWorker.syncSchedules(context, forceReplace = true)
                            isFinishing = false
                            onSetupCompleted()
                        }
                    },
                    enabled = !isFinishing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrightGold,
                        contentColor = Color(0xFF1A1400)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    if (isFinishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = Color(0xFF1A1400)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Memuat Turun Jadual JAKIM...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    } else {
                        Text(
                            text = "Simpan & Mula Guna AzanMY",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    if (showZoneSheet) {
        ZonePickerSheet(
            selectedZoneCode = selectedZone.code,
            onZoneSelected = { zone: JakimZone ->
                selectedZone = zone
                gpsMessage = "Zon dipilih: ${zone.shortTitle}"
                showZoneSheet = false
            },
            onDismiss = { showZoneSheet = false }
        )
    }
}

@Composable
private fun SetupPermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardEmerald,
        border = BorderStroke(
            width = 1.dp,
            color = if (isGranted) WarmGold.copy(alpha = 0.6f) else SubtleMintBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) BrightGold else MutedMintText,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    color = OffWhiteText,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = MutedMintText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            if (isGranted) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Aktif",
                    tint = Color(0xFF4ADE80),
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftGoldContainer,
                        contentColor = BrightGold
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
