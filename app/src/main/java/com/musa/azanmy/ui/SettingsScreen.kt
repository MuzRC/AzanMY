package com.musa.azanmy.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.musa.azanmy.BuildConfig
import com.musa.azanmy.alarm.PrayerSyncWorker
import com.musa.azanmy.data.AzanAudioCatalog
import com.musa.azanmy.data.AzanAudioOption
import com.musa.azanmy.data.JakimZoneData
import com.musa.azanmy.data.PrayerPrefs
import com.musa.azanmy.data.ThemePreference
import com.musa.azanmy.data.UserSettingsState
import com.musa.azanmy.service.AzanAudioPlayer
import com.musa.azanmy.service.PrayerStatusNotification
import com.musa.azanmy.util.AppUpdater
import com.musa.azanmy.util.PermissionHelper
import com.musa.azanmy.util.UpdateCheckResult
import com.musa.azanmy.util.UpdateDownloadState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: UserSettingsState,
    prefs: PrayerPrefs,
    onOpenZonePicker: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ext = LocalExtendedColors.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val playingRawName by AzanAudioPlayer.playingPreviewRawName.collectAsState()
    val availableRelease by AppUpdater.availableRelease.collectAsState()
    val downloadState by AppUpdater.downloadState.collectAsState()

    var permStatus by remember { mutableStateOf(PermissionHelper.checkAll(context)) }
    val zone = remember(settings.zoneCode) { JakimZoneData.findByCode(settings.zoneCode) }

    var isSyncing by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    val isDownloadingApk = downloadState is UpdateDownloadState.Downloading

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permStatus = PermissionHelper.checkAll(context)
                if (PermissionHelper.canRequestPackageInstalls(context)) {
                    AppUpdater.consumeAutoInstallIfReady(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is UpdateDownloadState.ReadyToInstall -> {
                if (!state.autoLaunchConsumed &&
                    lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                    PermissionHelper.canRequestPackageInstalls(context)
                ) {
                    AppUpdater.consumeAutoInstallIfReady(context)
                }
            }
            is UpdateDownloadState.Error -> {
                onShowMessage(state.message)
            }
            else -> Unit
        }
    }

    val lastSyncLabel = remember(settings.lastSyncMillis) {
        if (settings.lastSyncMillis <= 0L) {
            "Belum pernah disegerakkan"
        } else {
            val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
            "Kemaskini terakhir: ${fmt.format(Date(settings.lastSyncMillis))}"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Tetapan & Audio Azan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Bahagian 1: Status Ketahanan Penggera
        item {
            SectionLabel("Status Ketahanan Sistem")
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ext.cardBackground,
                border = BorderStroke(1.dp, ext.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DiagnosticRow(
                        label = "Penggera Tepat (Alarms & Reminders)",
                        isOk = permStatus.hasExactAlarm,
                        fixText = "Aktifkan",
                        onFix = { PermissionHelper.openExactAlarmSettings(context) }
                    )
                    DiagnosticRow(
                        label = "Abaikan Pengoptimuman Bateri",
                        isOk = permStatus.isBatteryUnrestricted,
                        fixText = "Nyahhad",
                        onFix = { PermissionHelper.requestIgnoreBatteryOptimizations(context) }
                    )
                    DiagnosticRow(
                        label = "Kebenaran Notifikasi",
                        isOk = permStatus.hasNotification,
                        fixText = "Tetapan",
                        onFix = { PermissionHelper.openAppDetailsSettings(context) }
                    )

                    OutlinedButton(
                        onClick = { PermissionHelper.openAppDetailsSettings(context) },
                        border = BorderStroke(1.dp, ext.cardBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tetapan Lanjutan Bateri & Auto-Launch (App Info)",
                            fontSize = 12.sp,
                            color = ext.subtleText
                        )
                    }
                }
            }
        }

        // Bahagian 2: Azan Subuh Khas (Tetap)
        item {
            SectionLabel("Laungan Khas Waktu Subuh (Tetap)")
            Spacer(modifier = Modifier.height(6.dp))
            val subuhTrack = AzanAudioCatalog.subuhFixedTrack
            val isSubuhPlaying = playingRawName == subuhTrack.rawName
            val isSubuhUploaded = remember {
                AzanAudioCatalog.isAudioUploaded(context, subuhTrack.rawName)
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ext.cardBackground,
                border = BorderStroke(1.dp, ext.goldAccent.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subuhTrack.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isSubuhUploaded) {
                                "Fail subuh.mp3 sedia • Dikunci khas untuk Subuh"
                            } else {
                                "Menunggu subuh.mp3 (Guna nada alarm sementara)"
                            },
                            fontSize = 12.sp,
                            color = ext.goldAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            AzanAudioPlayer.togglePreview(
                                context = context,
                                rawName = subuhTrack.rawName,
                                autoBoostVolume = settings.autoBoostAlarmVolume
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ext.goldContainer,
                            contentColor = ext.goldBright
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isSubuhPlaying) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSubuhPlaying) "Henti" else "Uji",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bahagian 3: Pilihan Suara Laungan Azan Harian
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("Pilihan Suara Azan Harian")
                Text(
                    text = "STREAM_ALARM",
                    fontSize = 11.sp,
                    color = ext.goldAccent,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "Untuk waktu Zohor, Asar, Maghrib & Isyak (Ketik untuk uji & pilih)",
                fontSize = 12.sp,
                color = ext.subtleText
            )
        }

        items(AzanAudioCatalog.dailyOptions) { option: AzanAudioOption ->
            val isSelected = settings.selectedDailyAzanId == option.id
            val isPlaying = playingRawName == option.rawName

            DailyAzanOptionCard(
                option = option,
                isSelected = isSelected,
                isPlaying = isPlaying,
                onSelectAndPreview = {
                    scope.launch {
                        prefs.setSelectedDailyAzanId(option.id)
                    }
                    AzanAudioPlayer.togglePreview(
                        context = context,
                        rawName = option.rawName,
                        autoBoostVolume = settings.autoBoostAlarmVolume
                    )
                }
            )
        }

        // Kad Suis Auto-Naikkan Volume Penggera
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ext.cardBackground,
                border = BorderStroke(1.dp, ext.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = "Auto-Naikkan Volume Penggera (80%)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Naikkan bunyi penggera ke 80% secara automatik jika di bawah 80% dan kekalkan tahap tersebut",
                            fontSize = 12.sp,
                            color = ext.subtleText
                        )
                    }
                    Switch(
                        checked = settings.autoBoostAlarmVolume,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                prefs.setAutoBoostAlarmVolume(enabled)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF1A1400),
                            checkedTrackColor = BrightGold
                        )
                    )
                }
            }
        }

        // Bahagian 4: Zon JAKIM, Notifikasi Status & Tema
        item {
            SectionLabel("Jadual JAKIM & Paparan")
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ext.cardBackground,
                border = BorderStroke(1.dp, ext.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Zon JAKIM
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenZonePicker)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = ext.goldBright,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Zon Waktu Solat (${zone.code})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = zone.areas,
                                fontSize = 12.sp,
                                color = ext.subtleText,
                                maxLines = 2
                            )
                        }
                        Text(
                            text = "Tukar",
                            color = ext.goldBright,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Butang Kemas Kini Manual Jadual JAKIM
                    Column {
                        Button(
                            onClick = {
                                scope.launch {
                                    isSyncing = true
                                    val res = PrayerSyncWorker.syncSchedules(context, forceReplace = true)
                                    res.onSuccess {
                                        onShowMessage("Jadual waktu solat JAKIM berjaya dikemas kini.")
                                    }.onFailure { err ->
                                        onShowMessage(err.message ?: "Gagal menyemak data JAKIM.")
                                    }
                                    isSyncing = false
                                }
                            },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ext.goldContainer,
                                contentColor = ext.goldBright
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = ext.goldBright
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kemas Kini Data JAKIM Sekarang",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$lastSyncLabel (Auto-semak setiap 2 hari & 20hb)",
                            fontSize = 11.sp,
                            color = ext.subtleText
                        )
                    }

                    // Suis Bar Notifikasi Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Papar Jadual di Bar Notifikasi",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Paparan senyap waktu seterusnya di panel notifikasi atas",
                                fontSize = 12.sp,
                                color = ext.subtleText
                            )
                        }
                        Switch(
                            checked = settings.showStatusNotification,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    prefs.setShowStatusNotification(enabled)
                                    if (enabled) {
                                        PrayerStatusNotification.update(context)
                                    } else {
                                        PrayerStatusNotification.cancel(context)
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF1A1400),
                                checkedTrackColor = BrightGold
                            )
                        )
                    }

                    // Pilihan Tema
                    Column {
                        Text(
                            text = "Tema Antaramuka",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemePreference.entries.forEach { theme ->
                                val selected = settings.themePreference == theme
                                Surface(
                                    onClick = {
                                        scope.launch {
                                            prefs.setThemePreference(theme)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) ext.goldContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) ext.goldAccent else ext.cardBorder
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 9.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = theme.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) ext.goldBright else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bahagian 5: Kemas Kini Aplikasi (GitHub Releases)
        item {
            SectionLabel("Kemas Kini Aplikasi")
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ext.cardBackground,
                border = BorderStroke(1.dp, ext.cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AzanMY v${BuildConfig.VERSION_NAME}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kod Binaan #${BuildConfig.VERSION_CODE} • GitHub MuzRC/AzanMY",
                                fontSize = 12.sp,
                                color = ext.subtleText
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = null,
                            tint = ext.goldBright
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isCheckingUpdate = true
                                when (val result = AppUpdater.checkForUpdate()) {
                                    is UpdateCheckResult.UpdateAvailable -> {
                                        onShowMessage("Versi baharu v${result.info.versionName} tersedia!")
                                    }
                                    is UpdateCheckResult.UpToDate -> {
                                        onShowMessage("Anda sedang menggunakan versi terkini (v${result.currentVersionName}).")
                                    }
                                    is UpdateCheckResult.Error -> {
                                        onShowMessage(result.message)
                                    }
                                }
                                isCheckingUpdate = false
                            }
                        },
                        enabled = !isCheckingUpdate && !isDownloadingApk,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ext.goldContainer,
                            contentColor = ext.goldBright
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = ext.goldBright
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "Semak Kemas Kini Baharu",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    val displayRelease = availableRelease
                        ?: (downloadState as? UpdateDownloadState.Downloading)?.releaseInfo
                        ?: (downloadState as? UpdateDownloadState.ReadyToInstall)?.releaseInfo
                        ?: (downloadState as? UpdateDownloadState.Error)?.releaseInfo

                    displayRelease?.let { release ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, ext.goldAccent)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Versi Baharu: v${release.versionName}",
                                    fontWeight = FontWeight.Bold,
                                    color = ext.goldBright,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = release.releaseNotes,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                when (val dlState = downloadState) {
                                    is UpdateDownloadState.Downloading -> {
                                        if (dlState.totalBytes > 0L) {
                                            LinearProgressIndicator(
                                                progress = { dlState.progress / 100f },
                                                color = BrightGold,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            LinearProgressIndicator(
                                                color = BrightGold,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        val progressLabel = if (dlState.totalBytes > 0L) {
                                            "${dlState.statusMessage} ${dlState.progress}% • " +
                                                "${AppUpdater.formatBytes(dlState.downloadedBytes)} / ${AppUpdater.formatBytes(dlState.totalBytes)}"
                                        } else {
                                            dlState.statusMessage
                                        }
                                        Text(
                                            text = progressLabel,
                                            fontSize = 12.sp,
                                            color = ext.goldBright,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Muat turun berjalan di latar belakang walaupun skrin padam.",
                                            fontSize = 11.sp,
                                            color = ext.subtleText
                                        )
                                        OutlinedButton(
                                            onClick = { AppUpdater.cancelBackgroundDownload(context) },
                                            border = BorderStroke(1.dp, ext.cardBorder),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Batal Muat Turun",
                                                fontSize = 12.sp,
                                                color = ext.subtleText
                                            )
                                        }
                                    }

                                    is UpdateDownloadState.ReadyToInstall -> {
                                        Text(
                                            text = "Muat turun selesai. Fail APK sedia dipasang.",
                                            fontSize = 12.sp,
                                            color = ext.statusOk,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Button(
                                            onClick = {
                                                if (!PermissionHelper.canRequestPackageInstalls(context)) {
                                                    onShowMessage("Sila benarkan pemasangan aplikasi dari sumber ini dahulu.")
                                                    PermissionHelper.openInstallUnknownAppsSettings(context)
                                                    return@Button
                                                }
                                                val ok = AppUpdater.installReadyApk(context)
                                                if (!ok) {
                                                    onShowMessage("Gagal membuka pemasang APK.")
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = BrightGold,
                                                contentColor = Color(0xFF1A1400)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Pasang Kemas Kini Sekarang",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    else -> {
                                        Button(
                                            onClick = {
                                                if (!PermissionHelper.canRequestPackageInstalls(context)) {
                                                    onShowMessage("Sila benarkan pemasangan aplikasi dari sumber ini dahulu.")
                                                    PermissionHelper.openInstallUnknownAppsSettings(context)
                                                    return@Button
                                                }
                                                AppUpdater.startBackgroundDownload(context, release)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = BrightGold,
                                                contentColor = Color(0xFF1A1400)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Muat Turun & Pasang Sekarang",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun DiagnosticRow(
    label: String,
    isOk: Boolean,
    fixText: String,
    onFix: () -> Unit
) {
    val ext = LocalExtendedColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isOk) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = if (isOk) ext.statusOk else ext.statusWarn,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (isOk) {
            Text(
                text = "Sedia",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ext.statusOk
            )
        } else {
            Surface(
                onClick = onFix,
                shape = RoundedCornerShape(8.dp),
                color = ext.goldContainer
            ) {
                Text(
                    text = fixText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ext.goldBright,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DailyAzanOptionCard(
    option: AzanAudioOption,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelectAndPreview: () -> Unit
) {
    val ext = LocalExtendedColors.current

    Surface(
        onClick = onSelectAndPreview,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) ext.goldContainer else ext.cardBackground,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) ext.goldAccent else ext.cardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = option.muadzin,
                    fontSize = 12.sp,
                    color = if (isSelected) ext.goldAccent else ext.subtleText
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) {
                        Color(0x33D4AF37)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPlaying) {
                            Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = "Henti",
                                tint = ext.goldBright,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Henti",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ext.goldBright
                            )
                        } else {
                            Text(
                                text = if (isSelected) "Aktif" else "Uji & Pilih",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) ext.goldBright else ext.subtleText
                            )
                        }
                    }
                }

                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Aktif",
                        tint = BrightGold,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
