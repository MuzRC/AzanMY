package com.musa.azanmy.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ext.screenGlowTop,
                        MaterialTheme.colorScheme.background,
                        ext.screenGlowBottom
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Baris Atas: Lencana Masjid Emas + Tajuk Metallic Gold & Pemilih Zon
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(6.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFF184B3F), Color(0xFF081C17))
                                    )
                                )
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            PaleGoldHighlight,
                                            WarmGold,
                                            DeepBronzeGold
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = "Logo AzanMY",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AzanMY",
                            style = TextStyle(
                                fontSize = 23.sp,
                                fontWeight = FontWeight.ExtraBold,
                                brush = if (ext.isDark) {
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            PaleGoldHighlight,
                                            BrightGold,
                                            Color(0xFFB38620)
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF0E5A47),
                                            Color(0xFF073328)
                                        )
                                    )
                                },
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.45f),
                                    offset = Offset(0f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )
                    }

                    // Pil Pemilih Zon Berbingkai Emas
                    Box(
                        modifier = Modifier
                            .shadow(6.dp, RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(ext.cardGradientTop, ext.cardGradientBottom)
                                )
                            )
                            .border(
                                width = 1.2.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        ext.goldBright.copy(alpha = 0.85f),
                                        ext.cardRimBottom
                                    )
                                ),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .clickable(onClick = onOpenZonePicker)
                            .padding(horizontal = 13.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = ext.goldBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = zone.shortTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(3.dp))
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

            // Kad Utama Hero Countdown dengan Mandala Islamik & Cahaya Keemasan
            item {
                val heroShape = RoundedCornerShape(24.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, heroShape)
                        .clip(heroShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(ext.heroStart, ext.heroEnd)
                            )
                        )
                        .border(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    PaleGoldHighlight.copy(alpha = 0.85f),
                                    WarmGold.copy(alpha = 0.70f),
                                    DeepBronzeGold.copy(alpha = 0.55f)
                                )
                            ),
                            shape = heroShape
                        )
                ) {
                    // Corak Mandala Geometri Islamik di Latar Belakang Kad
                    Image(
                        painter = painterResource(id = R.drawable.bg_islamic_pattern),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )

                    // Pancaran Cahaya Emas Halus di Puncak Atas Kad
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        WarmGold.copy(alpha = 0.22f),
                                        Color.Transparent
                                    ),
                                    radius = 420f
                                )
                            )
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
                                letterSpacing = 1.1.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = nextPrayer.prayer.displayName,
                                    style = TextStyle(
                                        color = OffWhiteText,
                                        fontSize = 33.sp,
                                        fontWeight = FontWeight.Bold,
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.45f),
                                            offset = Offset(0f, 2f),
                                            blurRadius = 6f
                                        )
                                    )
                                )
                                Text(
                                    text = nextPrayer.time12,
                                    style = TextStyle(
                                        fontSize = 27.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                PaleGoldHighlight,
                                                BrightGold,
                                                Color(0xFFC59626)
                                            )
                                        ),
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.45f),
                                            offset = Offset(0f, 2f),
                                            blurRadius = 6f
                                        )
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val remainingMs = (nextPrayer.triggerMillis - nowMillis).coerceAtLeast(0L)
                            val countdownText = formatCountdown(remainingMs)
                            val pillShape = RoundedCornerShape(14.dp)

                            // Pil Kiraan Detik Emas 3D Berkilat
                            Box(
                                modifier = Modifier
                                    .shadow(6.dp, pillShape)
                                    .clip(pillShape)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFFD9A738),
                                                Color(0xFF9C6D14),
                                                Color(0xFF6B480A)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.2.dp,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                PaleGoldHighlight,
                                                Color(0xFF8C6212)
                                            )
                                        ),
                                        shape = pillShape
                                    )
                                    .padding(horizontal = 15.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "Berbaki $countdownText",
                                    style = TextStyle(
                                        color = Color(0xFFFFFBEB),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.55f),
                                            offset = Offset(0f, 1f),
                                            blurRadius = 3f
                                        )
                                    )
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

            // Info Syuruk Ringkas dengan Bingkai Kaca
            if (todayTimes != null) {
                item {
                    val syurukShape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, syurukShape)
                            .clip(syurukShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(ext.cardGradientTop, ext.cardGradientBottom)
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(ext.cardRimTop, ext.cardRimBottom)
                                ),
                                shape = syurukShape
                            )
                            .padding(horizontal = 16.dp, vertical = 11.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.WbTwilight,
                                    contentDescription = null,
                                    tint = ext.goldBright,
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
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Senarai 5 Waktu Fardhu dengan Kad Kaca-Metalik & Butang Emas 3D
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
    val cardShape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, cardShape)
            .clip(cardShape)
            .background(
                if (isHighlighted) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF234B3E),
                            Color(0xFF102A23)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(ext.cardGradientTop, ext.cardGradientBottom)
                    )
                }
            )
            .drawWithContent {
                drawContent()
                // Pantulan Kilauan Kaca Pepenjuru pada Separuh Kiri Kad
                val sheenPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width * 0.56f, 0f)
                    lineTo(size.width * 0.36f, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = sheenPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (ext.isDark) 0.065f else 0.30f),
                            Color.White.copy(alpha = 0.01f)
                        )
                    )
                )
            }
            .border(
                width = if (isHighlighted) 1.5.dp else 1.1.dp,
                brush = if (isHighlighted) {
                    Brush.verticalGradient(
                        colors = listOf(PaleGoldHighlight, WarmGold, DeepBronzeGold)
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(ext.cardRimTop, ext.cardRimBottom)
                    )
                },
                shape = cardShape
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prayer.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlighted) ext.goldHighlight else MaterialTheme.colorScheme.onSurface
                )
                if (isHighlighted) {
                    Text(
                        text = "Waktu seterusnya",
                        fontSize = 11.sp,
                        color = ext.goldBright
                    )
                }
            }

            Text(
                text = time12,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHighlighted) ext.goldHighlight else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 12.dp)
            )

            // Butang Pil Mod Bunyi 3D Berkilat
            val btnShape = RoundedCornerShape(14.dp)
            val btnBackground = when (alertMode) {
                AzanAlertMode.AZAN -> Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFA87B22),
                        Color(0xFF63460D),
                        Color(0xFF422E07)
                    )
                )
                AzanAlertMode.VIBRATE -> Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF215A4C),
                        Color(0xFF11352C)
                    )
                )
                AzanAlertMode.SILENT -> Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A362F),
                        Color(0xFF0E211C)
                    )
                )
            }
            val btnBorder = when (alertMode) {
                AzanAlertMode.AZAN -> Brush.verticalGradient(
                    colors = listOf(PaleGoldHighlight, Color(0xFF8C6212))
                )
                AzanAlertMode.VIBRATE -> Brush.verticalGradient(
                    colors = listOf(Color(0xFF8CE0C5), Color(0xFF26594C))
                )
                AzanAlertMode.SILENT -> Brush.verticalGradient(
                    colors = listOf(Color(0xFF3E685D), Color(0xFF1D3831))
                )
            }

            Box(
                modifier = Modifier
                    .shadow(4.dp, btnShape)
                    .clip(btnShape)
                    .background(btnBackground)
                    .border(width = 1.1.dp, brush = btnBorder, shape = btnShape)
                    .clickable(onClick = onToggleMode)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (alertMode) {
                            AzanAlertMode.AZAN -> Icons.AutoMirrored.Rounded.VolumeUp
                            AzanAlertMode.VIBRATE -> Icons.Rounded.Vibration
                            AzanAlertMode.SILENT -> Icons.Rounded.NotificationsOff
                        },
                        contentDescription = alertMode.label,
                        tint = when (alertMode) {
                            AzanAlertMode.AZAN -> PaleGoldHighlight
                            AzanAlertMode.VIBRATE -> Color(0xFFBDF5E3)
                            AzanAlertMode.SILENT -> MutedMintText
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = when (alertMode) {
                            AzanAlertMode.AZAN -> "Azan"
                            AzanAlertMode.VIBRATE -> "Getar"
                            AzanAlertMode.SILENT -> "Senyap"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (alertMode) {
                            AzanAlertMode.AZAN -> Color(0xFFFFF5D1)
                            AzanAlertMode.VIBRATE -> Color(0xFFD9FFF3)
                            AzanAlertMode.SILENT -> MutedMintText
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
