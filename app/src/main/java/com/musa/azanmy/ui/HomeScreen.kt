package com.musa.azanmy.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musa.azanmy.R
import com.musa.azanmy.data.AzanAlertMode
import com.musa.azanmy.data.DailyPrayerTimes
import com.musa.azanmy.data.JakimZoneData
import com.musa.azanmy.data.PrayerName
import com.musa.azanmy.data.PrayerTimeCalculator
import com.musa.azanmy.data.UserSettingsState
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    settings: UserSettingsState,
    todayTimes: DailyPrayerTimes?,
    tomorrowTimes: DailyPrayerTimes?,
    onOpenZonePicker: () -> Unit,
    onTogglePrayerMode: (PrayerName, AzanAlertMode) -> Unit,
    onManualSyncRequested: () -> Unit
) {
    val ext = LocalExtendedColors.current
    val zone = remember(settings.zoneCode) { JakimZoneData.findByCode(settings.zoneCode) }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isSyncingEmpty by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val nextPrayer = remember(todayTimes, tomorrowTimes, nowMillis / 1000L) {
        PrayerTimeCalculator.findNextPrayer(todayTimes, tomorrowTimes, nowMillis)
    }

    val gregorianLabel = remember(todayTimes) {
        try {
            val date = if (todayTimes != null) LocalDate.parse(todayTimes.dateIso) else LocalDate.now()
            val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
            val dayPrefix = todayTimes?.dayName ?: ""
            if (dayPrefix.isNotBlank()) "$dayPrefix, ${date.format(fmt)}" else date.format(fmt)
        } catch (_: Exception) {
            LocalDate.now().toString()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Baris Atas: Jenama & Pemilih Zon
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ext.goldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AzanMY",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Surface(
                    onClick = onOpenZonePicker,
                    shape = RoundedCornerShape(20.dp),
                    color = ext.cardBackground,
                    border = BorderStroke(1.dp, ext.cardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = ext.goldBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = zone.shortTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = ext.subtleText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Kad Utama Hero Countdown
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.55f)),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ext.heroStart, ext.heroEnd)
                            )
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bg_islamic_pattern),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = gregorianLabel,
                                color = MutedMintText,
                                fontSize = 12.sp
                            )
                            Text(
                                text = todayTimes?.hijriFormatted?.takeIf { it.isNotBlank() }
                                    ?: "Jadual JAKIM",
                                color = BrightGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (nextPrayer != null) {
                            Text(
                                text = if (nextPrayer.isTomorrow) {
                                    "WAKTU SOLAT ESOK"
                                } else {
                                    "WAKTU SOLAT SETERUSNYA"
                                },
                                color = MutedMintText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = nextPrayer.prayer.displayName,
                                    color = OffWhiteText,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = nextPrayer.time12,
                                    color = BrightGold,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val remainingMs = (nextPrayer.triggerMillis - nowMillis).coerceAtLeast(0L)
                            val countdownText = formatCountdown(remainingMs)

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x26D4AF37)
                            ) {
                                Text(
                                    text = "Berbaki $countdownText",
                                    color = BrightGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "Tiada Data Jadual Offline",
                                color = OffWhiteText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sila muat turun jadual waktu solat rasmi JAKIM untuk zon ${zone.code}.",
                                color = MutedMintText,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    isSyncingEmpty = true
                                    onManualSyncRequested()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrightGold,
                                    contentColor = Color(0xFF1A1400)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSyncingEmpty && todayTimes == null) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF1A1400)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Muat Turun Sekarang", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Info Syuruk Ringkas
        if (todayTimes != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ext.cardBackground,
                    border = BorderStroke(1.dp, ext.cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WbTwilight,
                                contentDescription = null,
                                tint = ext.goldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Waktu Syuruk (Tamat Subuh)",
                                fontSize = 13.sp,
                                color = ext.subtleText
                            )
                        }
                        Text(
                            text = todayTimes.getTime12For(PrayerName.SYURUK),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Senarai 5 Waktu Fardhu
            items(PrayerName.fardhuList) { prayer ->
                val isNext = nextPrayer != null && !nextPrayer.isTomorrow && nextPrayer.prayer == prayer
                val alertMode = settings.alertModes[prayer] ?: AzanAlertMode.AZAN

                PrayerRowCard(
                    prayer = prayer,
                    time12 = todayTimes.getTime12For(prayer),
                    isHighlighted = isNext,
                    alertMode = alertMode,
                    onToggleMode = {
                        onTogglePrayerMode(prayer, alertMode.next())
                    }
                )
            }
        }
    }
}

@Composable
private fun PrayerRowCard(
    prayer: PrayerName,
    time12: String,
    isHighlighted: Boolean,
    alertMode: AzanAlertMode,
    onToggleMode: () -> Unit
) {
    val ext = LocalExtendedColors.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isHighlighted) ext.goldContainer else ext.cardBackground,
        border = BorderStroke(
            width = if (isHighlighted) 1.5.dp else 1.dp,
            color = if (isHighlighted) ext.goldAccent else ext.cardBorder
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
                    text = prayer.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted) ext.goldBright else MaterialTheme.colorScheme.onSurface
                )
                if (isHighlighted) {
                    Text(
                        text = "Waktu seterusnya",
                        fontSize = 11.sp,
                        color = ext.goldAccent
                    )
                }
            }

            Text(
                text = time12,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHighlighted) ext.goldBright else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 12.dp)
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (alertMode) {
                    AzanAlertMode.AZAN -> ext.goldContainer
                    AzanAlertMode.VIBRATE -> MaterialTheme.colorScheme.secondaryContainer
                    AzanAlertMode.SILENT -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onToggleMode)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (alertMode) {
                            AzanAlertMode.AZAN -> Icons.AutoMirrored.Rounded.VolumeUp
                            AzanAlertMode.VIBRATE -> Icons.Rounded.Vibration
                            AzanAlertMode.SILENT -> Icons.Rounded.NotificationsOff
                        },
                        contentDescription = alertMode.label,
                        tint = when (alertMode) {
                            AzanAlertMode.AZAN -> ext.goldBright
                            AzanAlertMode.VIBRATE -> MaterialTheme.colorScheme.onSecondaryContainer
                            AzanAlertMode.SILENT -> ext.subtleText
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (alertMode) {
                            AzanAlertMode.AZAN -> "Azan"
                            AzanAlertMode.VIBRATE -> "Getar"
                            AzanAlertMode.SILENT -> "Senyap"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (alertMode) {
                            AzanAlertMode.AZAN -> ext.goldBright
                            AzanAlertMode.VIBRATE -> MaterialTheme.colorScheme.onSecondaryContainer
                            AzanAlertMode.SILENT -> ext.subtleText
                        }
                    )
                }
            }
        }
    }
}

private fun formatCountdown(millis: Long): String {
    val totalSeconds = millis / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.ENGLISH, "%02dj %02dm %02ds", hours, minutes, seconds)
}
